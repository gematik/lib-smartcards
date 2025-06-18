# Introduction

This (sub)-project contains utility classes used and useful for communication
with an [ICC][] via [PC/SC][]. Calls to native libraries use [JNA][].

Several libraries already exist to communicate with an [ICC][] from JAVA, e.g.:
1. Sun's provider, which is the default, see
   `javax.smartcardio.TerminalFactory.getDefault()`.
2. [jnasmartcardio][] which uses [JNA][] and is otherwise quite similar to Sun's
   approach (when seen from the API).

**Why another library with similar functionality?**
1. I want to have FULL control over the code and its features.
2. I miss features in the classes from the other libraries.
3. What I miss most is:
   1. using a connection as a layer (see `de.gematik.smartcards.sdcom.apdu.ApduLayer`)
   2. logging
   3. response buffer greater than  8 kiByte
   4. full control over command APDU, e.g. `MANAGE CHANNEL` command
   5. sub-classing `CommandAPDU`
   6. more functionality in the `ATR` class

# Approach
The library here uses mechanisms form [jnasmartcardio][] when calling native
libraries. The class structure and their implementation are more similar to
Sun's code.

# Differences
This section describes what is available and where the differences to Suns's
implementation are.

# General
Parameters (in and out) of library calls are logged at `TRACE` level.

# Class `AfiPcsc`
The class `AfiPcsc` contains a provider which can be used when calling
`TerminalFactory.getInstance(String, Object, Provider)`.

# Class `IfdFactory`
The class `IfdFactory` extends `TerminalFactorySpi` such that a usable instance
of class `CardTerminals` can be retrieved via `TerminalFactory.getTerminals()`.

# Class `IfdCollection`
The class `IfdCollection` extends `CardTerminals`.

# Difference to Suns's implementation
1. The class `IfdCollection` implements interface `Autoclosable`.
   Thus, resources, which have been allocated upon constructing an instance
   (via `SCardEstablishContext`), are releases (via `SCardReleaseContext`).
2. The class `IfdCollection` establishes a context with _SCARD_SCOPE_SYSTEM_,
   whereas Sun's implementation uses _SCARD_SCOPE_USER_, see source code of class 
   `sun.security.smartcardio.PSCSTerminals` and there method `initContext()`.
3. The class `IfdCollection` logs function calls at `DEBUG` level. 

## TODO
1. Method `list(State)` in  class `IfdCollection` does not (yet) support
   states from set {CARD_INSERTION, CARD_REMOVAL}.
2. Method `waitForChange(long)` in  class `IfdCollection` is not (yet) implemented.

# Class `Ifd`
The class `Ifd` extends `CardTerminal`. This class does not contribute to
log-files, because actions and method calls are sufficiently logged by other
classes.

# Class `Icc`
The class `Icc` extends `Card`. Furthermore, it implements the interface
`ApduLayer`. In contrast to class `IccChannel` which also implements interface
`ApduLayer` the overriden methods `send(byte[])` and `send(CommandApdu)` permit
any `CommandApdu`. In particular those methods do not prohibit `MANAGE CHANNEL`
commands. This way, by using these methods from class `Icc` a user has full
control over the command response interface to a smart card.

From an applications point of view, there is no need to issue `MANAGE CHANNEL`
commands via `send(CommandApdu)` or `send(byte[])`.
1. For opening a (new) logical channel use method `openLogicalChannel()` from
   class `Icc`. Upon success that method returns a newly opened logical channel.
2. For closing a logical channel (other than the basic logical channel) use
   method `close()` from class `IccChannel`.
3. For resetting a logical channel use method `reset()` from class `IccChannel`.
4. For resetting the application layer (i.e. closing all logical channels and
   resetting the basic logical channel) use method `reset()` from class `Icc`.

## Differences to Sun's implementation
1. The class `Icc` connects to a smart card in mode `SCARD_SHARE_EXCLUSIVE`
   whereas Sun's implementation uses mode `SCARD_SHARE_SHARED`.
2. The class `Icc` provides a method `getAnswerToReset()` which returns an
   instance of class `AnswerToReset`. That class encapsulates important
   information from an Answer-To-Reset.
3. The class `Icc` logs `CommandApdu` and `ResponseApdu` information in method
   `send(CommandApdu)` at `DEBUG` level.
4. The class `Icc` logs octet-string representations of command and response
   APDU in method `send(byte[]` at `TRACE` level. That is the same level at
   which parameters of library calls are logged.

## TODO
1. The class `Icc` supports protocol "T=1" only. In particular protocol "T=0" is
   not (yet) supported.
2. When method `disconnect(boolean)` is called with value `reset = true` then
   class `Icc` uses `SCARD_UNPOWER_CARD` versus Sun uses `SCARD_RESET_CARD`
   which causes a warm-reset.
3. Methods `beginExclusive()` and `endExclusive()` in class `Icc` are not (yet)
   supported.
4. Method `transmitControlCommand(int, byte[])` in class `Icc` is not (yet)
   supported.
   
# Class `IccChannel`
The class `IccChannel` extends `CardChannel`. Furthermore, it implements interface
`ApduLayer`. In contrast to class `Icc` which also implements interface
`ApduLayer` the overriden methods `send(byte[])` and `send(CommandApdu)` prohibit
any `MANAGE CHANNEL` command. See the description for clas `Icc` above how to
manage logical channels.

## Differences to Sun's implementation
1. The method `transimt(ByteBuffer, ByteBuffer)` in class `IccChannel` throws an
   `IllegalArgumentExcpetion` if the `remaining()` for the _response_ is less
   than the length of the received response APDU, whereas Sun's implementation
   throws such an exception in case `remaining()` for the _response_ is less
   than 258.
2. The `toString()`  method in class `IccChannel` includes the name of the `Icc`
   whereas Sun's implementation only shows the channel number.

# System overview

## Package `javax.smartcardio` with Sun's Provider

Here is a code example using `javax.smartcardio` and Sun's provider. The following
code reads the content of EF.GDO from the first IFD where an ICC is present:

```java
    try {
      final TerminalFactory terminalFactory = TerminalFactory.getDefault();
      final CardTerminals cardTerminals = terminalFactory.terminals();
      final CardTerminal ifd = cardTerminals.list(CardTerminals.State.CARD_PRESENT).getFirst();
      final Card icc = ifd.connect("T=1");
      final CardChannel cardChannel = icc.getBasicChannel();
      final CommandAPDU cmdApdu = new CommandAPDU(Hex.toByteArray("00 b0 8200 00"));
      final ResponseAPDU rspApdu = cardChannel.transmit(cmdApdu);

      LOGGER.atInfo().log(
          "read EF.GDO, response APDU: {}",
          new ResponseApdu(rspApdu.getBytes()).toString()
      );

      assertEquals("SunPCSC", terminalFactory.getProvider().getName());
      assertEquals("javax.smartcardio.TerminalFactory", terminalFactory.getClass().getName());
      assertEquals("sun.security.smartcardio.PCSCTerminals", cardTerminals.getClass().getName());
      assertEquals("sun.security.smartcardio.TerminalImpl", ifd.getClass().getName());
      assertEquals("sun.security.smartcardio.CardImpl", icc.getClass().getName());
      assertEquals("sun.security.smartcardio.ChannelImpl", cardChannel.getClass().getName());
    } catch (CardException e) {
      fail("UNEXPECTED", e);
    } // end catch (...)
```

## Package `javax.smartcardio` with "AfiPcsc"-Provider

Here is a code example using `javax.smartcardio` and "AfiPcsc"-provider. The following
code reads the content of EF.GDO from the first IFD where an ICC is present:

```java
    try {
      final TerminalFactory terminalFactory = TerminalFactory.getInstance(
          AfiPcsc.TYPE,
          null,
          new AfiPcsc()
      );
      final CardTerminals cardTerminals = terminalFactory.terminals();
      final CardTerminal ifd = cardTerminals.list(CardTerminals.State.CARD_PRESENT).getFirst();
      final Card icc = ifd.connect("T=1");
      final CardChannel cardChannel = icc.getBasicChannel();
      final CommandAPDU cmdApdu = new CommandAPDU(Hex.toByteArray("00 b0 8200 00"));
      final ResponseAPDU rspApdu = cardChannel.transmit(cmdApdu);
      
      icc.disconnect(true);
      
      LOGGER.atInfo().log(
          "read EF.GDO, response APDU: {}",
          new ResponseApdu(rspApdu.getBytes()).toString()
      );
      
      assertEquals("de.gematik.smartcards.pcsc.AfiPcsc", terminalFactory.getProvider().getName());
      assertEquals("javax.smartcardio.TerminalFactory", terminalFactory.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.IfdCollection", cardTerminals.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.Ifd", ifd.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.Icc", icc.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.IccChannel", cardChannel.getClass().getName());
    } catch (CardException | NoSuchAlgorithmException e) {
      fail("UNEXPECTED", e);
    } // end catch (...)
```

## Module `de.gematik.smartcards.pcsc`

Here is a code example using the module `de.gematik.smartcards.pcsc`. The following
code reads the content of EF.GDO from the first IFD where an ICC is present:

```java
    try (var ifdCollection = new IfdCollection()) {
      final Ifd ifd = (Ifd) ifdCollection.list(CardTerminals.State.CARD_PRESENT)
          .getFirst();
      final Icc icc = (Icc) ifd.connect("T=1");
      final ResponseApdu rspApdu = icc.send(new ReadBinary(2, 0, CommandApdu.NE_SHORT_WILDCARD));

      icc.disconnect(true);

      LOGGER.atInfo().log(
          "read EF.GDO, response APDU: {}",
          rspApdu.toString()
      );
      assertEquals("de.gematik.smartcards.pcsc.Ifd", ifd.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.Icc", icc.getClass().getName());
    } catch (CardException e) {
      fail("UNEXPECTED", e);
    } // end catch (...)
```

[ICC]:https://en.wikipedia.org/wiki/Smart_card
[JNA]:https://github.com/java-native-access/jna
[PC/SC]:https://en.wikipedia.org/wiki/PC/SC
[jnasmartcardio]:https://github.com/jnasmartcardio/jnasmartcardio
