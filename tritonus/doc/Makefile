# Generated automatically from Makefile.in by configure.
# compilation and packing for the sinfonia library
#

LIBNAME=tritonus

JAVAC=javac
# JAVAC=guavac

%.class: %.java
	$(JAVAC) -d $(DESTDIR) $<


SRCDIR=src/javax/media/sound
SRCDIR2=src/org/gnu/$(LIBNAME)
DESTDIR=.
OBJDIR=$(DESTDIR)/javax/media/sound
OBJDIR2=$(DESTDIR)/org/gnu/$(LIBNAME)
PACKSRCDIR=javax/media/sound org/gnu/$(LIBNAME)
LIBDEST=$(LIBNAME).jar

vpath %.class $(OBJDIR)/midi
vpath %.class $(OBJDIR)/midi/spi
vpath %.class $(OBJDIR)/sampled
vpath %.class $(OBJDIR)/sampled/spi
vpath %.class $(OBJDIR2)
vpath %.class $(OBJDIR2)/nas

vpath %.java $(SRCDIR)/midi
vpath %.java $(SRCDIR)/midi/spi
vpath %.java $(SRCDIR)/sampled
vpath %.java $(SRCDIR)/sampled/spi
vpath %.java $(SRCDIR2)
vpath %.java $(SRCDIR2)/nas

SRCTMP:=$(wildcard \
	$(SRCDIR)/midi/*.java \
	$(SRCDIR)/midi/spi/*.java \
	$(SRCDIR)/sampled/*.java \
	$(SRCDIR)/sampled/spi/*.java \
	$(SRCDIR2)/*.java \
	$(SRCDIR2)/nas/*.java \
	)
SRC:=$(notdir $(SRCTMP))
OBJ:=$(SRC:.java=.class)

# documentation generation
DOCDESTDIR=./apidoc
DOCSRCPATH=./src
PACKAGES=javax.media.sound.midi \
	javax.media.sound.midi.spi \
	javax.media.sound.sampled \
	javax.media.sound.sampled.spi \
	org.gnu.$(LIBNAME)

# compilation for the mvp tests
TESTSRCDIR=src/mvptest
TESTDESTDIR=.
TESTSRC:=$(wildcard $(TESTSRCDIR)/*.java)

#----------------- rules -------------------

.PHONY: all
all: $(LIBDEST) $(NLIBDEST)

$(LIBDEST):	$(OBJ)
	jar cf $(LIBDEST) $(PACKSRCDIR)

$(NLIBDEST):	$(NLIBOBJS)
	ar cf $(NLIBDEST) $(NLIBOBJS)

.PHONY: rebuildall
rebuildall: clean
	$(JAVAC) -d $(DESTDIR) $(SRCTMP)
	jar cf $(LIBDEST) $(PACKSRCDIR)

test:	all $(TESTSRC)
	javac -d $(TESTDESTDIR) $(TESTSRC)

.PHONY: dotest
dotest:	test
	java test.ui.TestRunner mvptest.TestAll

doc: $(SRC)
	-mkdir $(DOCDESTDIR) 
	javadoc -author -splitindex -windowtitle "Sinfonia Documentation" -d $(DOCDESTDIR) -sourcepath $(DOCSRCPATH) $(PACKAGES)

.PHONY: clean
clean:
	rm -rf gnu mvptest apidoc

