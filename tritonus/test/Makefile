# Generated automatically from Makefile.in by configure.
# test of tritonus lib
#
# NOTE: to run these programs successfully, you must have Tritonus already
# installed. See the file INSTALL.
#

TESTCLASS=Sine
#SOUNDFILE=../../sounds/flute+hrn+mrmba.au
#SOUNDFILE=../../sounds/22-new.aif
#SOUNDFILE=../../sounds/welcome.wav
SOUNDFILE=../../sounds/spacemusic.au	# u-law
#SOUNDFILE=welcome.wav

JAVAC=javac
# JAVAC=guavac

%.class: %.java
	$(JAVAC) -d $(DESTDIR) $<

JAVAFLAGS=-Djava.compiler=NONE
PATHSETTINGS=export CLASSPATH=.:../tritonus.jar:$(CLASSPATH); export LD_LIBRARY_PATH=../src/lib/nas:../src/lib/esd

SRCDIR=.
DESTDIR=.
OBJDIR=$(DESTDIR)

vpath %.class $(OBJDIR)

vpath %.java $(SRCDIR)


SRCTMP:=$(wildcard \
	$(SRCDIR)/*.java \
	)
SRC:=$(notdir $(SRCTMP))
OBJ:=$(SRC:.java=.class)

#----------------- rules -------------------

.PHONY: all
all: $(OBJ)

.PHONY: test
test:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) Sine

.PHONY: testserver
testserver:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) ServerInfo

.PHONY: testnas
testnas:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) SineNas

.PHONY: testnas2
testnas2:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) SineNas2

.PHONY: testplayer
testplayer:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) Player $(SOUNDFILE)

.PHONY: testwiredump
testwiredump:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) WireDump

.PHONY: testformat
testformat:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) ShowFormat -i $(SOUNDFILE)

.PHONY: testconvert
testconvert:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) Convert $(SOUNDFILE)

.PHONY: testsysteminfo
testsysteminfo:	all
	$(PATHSETTINGS); java $(JAVAFLAGS) SystemInfo

.PHONY: install
install:

.PHONY: install-link
install-link:

.PHONY: clean
clean:
	rm -rf *.class *~
