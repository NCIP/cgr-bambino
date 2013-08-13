#RSYNC_SSH_HOST=sshuman.nci.nih.gov
RSYNC_SSH_HOST=ncias-p440-v.nci.nih.gov

bogus:
	@echo make all: build aceviewer
	@echo make avjar: build .jar files for Ace2.AceViewer -- bamview
	@echo make hmjar: build executable .jar file for TCGA.Heatmap6 -- heatmap viewer
	@echo make gvjar: build .jar files for Trace.GenotypeViewer
	@echo make dist: build viewer and create/distribute classfiles.

all:	
	(cd Funk && make)
	(cd Trace && make)
	(cd Ace2 && make)
#	(cd AceSelect && make)

clean:
	(cd Trace && make clean)
	(cd Funk && make clean)
	(cd Ace2 && make clean)
#	(cd AceSelect && make clean)

applet:	clean
	# for applet distribution, make sure we compile "classic"
	# applet classes with older JVM target (version 1.2)
	(cd Trace; make -f Makefile_applet)
	(cd Funk; make -f Makefile_applet)
	(cd Ace; make -f Makefile_applet)
#	(cd AceSelect; make -f Makefile_applet)

zips:
#	applet
	rm -f test.zip test9.zip
	zip -0 test Ace/*class Trace/*class Funk/*class AceSelect/*class
	zip -9 test9 Ace/*class Trace/*class Funk/*class AceSelect/*class
	pscp test.zip test9.zip edmonson@sshuman.nci.nih.gov:

localcheck:
	grep -i 'try_local.*false' Trace/StreamDelegator.java

chlc1check:
	grep 'chlc1' Funk/Net.java;

chlcfscheck:
	grep 'chlcfs' Funk/Net.java;

dist:	localcheck all zip zip9

lpgws: all zip zip9
#	rcp /tmp/test.zip lpgws.nci.nih.gov:/usr/local/apache/htdocs/cgap
#	rcp /tmp/test9.zip lpgws.nci.nih.gov:/usr/local/apache/htdocs/cgap
#  (obsolete)
	scp /tmp/test.zip lpgws.nci.nih.gov:/export/home/lpg-web/lpgweb/docs/mne_java
	scp /tmp/test9.zip lpgws.nci.nih.gov:/export/home/lpg-web/lpgweb/docs/mne_java

get:
#	rsync --progress -avuz edmonson@sshuman.nci.nih.gov:rsync/ .
	rsync --progress -avuz edmonson@${RSYNC_SSH_HOST}:rsync/ .

put:
#	rsync --progress -avuz . edmonson@sshuman.nci.nih.gov:rsync/
	rsync --progress -avuz . edmonson@${RSYNC_SSH_HOST}:rsync/

sync:	put get

sync_local_master:
	perl c:/me/bin/confirm "dangerous: DELETES REMOTE FILES not on this machine!  Continue? "
	rsync --delete -avuz . edmonson@sshuman.nci.nih.gov:rsync/
	rsync -avuz edmonson@sshuman.nci.nih.gov:rsync/ .

sync_local_slave:
	perl c:/me/bin/confirm "dangerous: DELETES LOCAL FILES not on remote machine!  Continue? "
	rsync -avuz --delete edmonson@sshuman.nci.nih.gov:rsync/ .
	rsync -avuz . edmonson@sshuman.nci.nih.gov:rsync/


gvjar:
# GenotypeViewer classes
#	jar cvfm gv.jar gtv.manifest Trace/*.class Funk/*.class
	(cd Trace; make)
	(cd Funk; make)
	jar cvfm gv.jar gtv.manifest Trace/*.class Funk/*.class
	cd third_party/unpacked/ && jar uvf ../../gv.jar org/
	pscp gv.jar edmonson@sshuman.nci.nih.gov:

hmjar:
# heatmap classes
	(cd Funk; make)
	(cd TCGA; make)
#	(cd TCGA; pod2html heatmap.pod --outfile heatmap.html)
#	(cd TCGA; perl c:/me/bin/pod_html_fix -file heatmap.html)
#	jar cvfm heatmap.jar heatmap.manifest TCGA/*.class Funk/*.class *.tab
#	jar cvfm heatmap.jar heatmap.manifest TCGA/*.class Funk/*.class
#	jar cvfm heatmap.jar heatmap.manifest TCGA/*.class TCGA/pathway_genes.tab.gz TCGA/heatmap.html Funk/Str.class Funk/LookAndFeeler.class Funk/DelimitedFile.class Funk/Gr.class Funk/Timer.class
	jar cvfm heatmap.jar heatmap.manifest TCGA/*.class TCGA/pathway_genes.tab.gz TCGA/gene_info.tab.gz Funk/Str.class Funk/LookAndFeeler.class Funk/DelimitedFile.class Funk/Gr.class Funk/Timer.class Funk/MenuBuilder.class Funk/Borders.class
	cd third_party/unpacked/ && jar uvf ../../heatmap.jar com/ layout/
	jarsigner -keystore myKeystore heatmap.jar myself
#	echo DEBUG: NOT COPYING .jar 
#	pscp heatmap.jar edmonson@sshuman.nci.nih.gov:

mf14:
	(cd third_party/src; echo -target jsr14 > mjm.config; perl c:/me/bin/mjm; make clean; make; cmd /c publish.bat)
	(cd Funk; echo -target jsr14 > mjm.config; perl c:/me/bin/mjm; make clean; make)
	(cd TCGA; echo -target jsr14 > mjm.config; perl c:/me/bin/mjm; make clean; make)

mf15:
	(cd third_party/src; echo -target 1.5 > mjm.config; perl c:/me/bin/mjm; make clean; make; cmd /c publish.bat)
	(cd Funk; echo -target 1.5 > mjm.config; perl c:/me/bin/mjm; make clean; make)
	(cd TCGA; echo -target 1.5 > mjm.config; perl c:/me/bin/mjm; make clean; make)

blobsrc:
	rm -f blobsrc.zip
	zip -9 blobsrc.zip Makefile *.manifest README_src.txt
	zip -r9 blobsrc third_party/unpacked/org/
	zip -r9 blobsrc third_party/unpacked/com/
	zip -r9 blobsrc third_party/unpacked/layout/
	zip -r9 blobsrc Ace2/*.java Ace2/Makefile Ace2/BambinoMuse.obt
	zip -r9 blobsrc Funk/*.java Funk/Makefile
	zip -r9 blobsrc Trace/*.java Trace/Makefile
	zip -r9 blobsrc TCGA/*.java TCGA/Makefile
	zip -r0 blobsrc TCGA/pathway_genes.tab.gz
	zip -r0 blobsrc TCGA/gene_info.tab.gz
	zip -d blobsrc Ace2/SplicedReadReporter.java
	zip -d blobsrc Ace2/SplicedReadFlankingInfo.java
	zip -d blobsrc Ace2/SplicedReadInfo.java
	zip -d blobsrc Ace2/TranscriptExonIntronCoverage.java
	zip -d blobsrc Ace2/ExonIntronCoverage.java
	zip -d blobsrc Ace2/MetaExonIntronCoverage.java

hmsrc:
# heatmap source
	rm -f hmsrc.zip
	cp Makefile_Funk_heatmap Funk/Makefile
	zip -9 hmsrc README_src.txt heatmap.manifest TCGA/Makefile TCGA/*.java TCGA/pathway_genes.tab.gz TCGA/gene_info.tab.gz Funk/Makefile Funk/Str.java Funk/LookAndFeeler.java Funk/DelimitedFile.java Funk/Gr.java Funk/Timer.java Funk/MenuBuilder.java
	zip -r9 hmsrc third_party/unpacked/com/ third_party/unpacked/layout/
	cd custom && zip -9 ../hmsrc Makefile
	rm -f Funk/Makefile

hmdocs:
# heatmap documentation
	rm -f heatmap_docs.zip
	(cd heatmap_docs; zip -9 ../heatmap_docs.zip *.jpg *.png *.html *.ppt)
	pscp heatmap_docs.zip edmonson@sshuman.nci.nih.gov:

avdocs:
# bamviewer documentation
	rm -f bamview_docs.zip
	(cd bamview_docs && zip -9 ../bamview_docs.zip *.png *.PNG *.html *.txt *.doc)
	pscp bamview_docs.zip edmonson@${RSYNC_SSH_HOST}:

hmdemo: hmjar
	zip -9 heatmap_demo.zip heatmap.jar *.tab

avjar_blob:
	(cd Funk; make)
	(cd Trace; make)
	(cd TCGA; make)
	(cd IsoView; make)
	(cd Ace2; make)
	(cd Ace2; java Ace2.BambinoProperties -increment-build -set-build-date)
	exit
	rm -f av_blob.jar
	jar cvfm av_blob.jar av.manifest IsoView/*.class Trace/*.class Funk/*.class Ace2/*.class Ace2/bambino.properties Ace2/BambinoMuse.obt TCGA/*.class
	cd third_party/unpacked/ && jar uvf ../../av_blob.jar com/ layout/

avjar_blob2: avjar_blob
	/cygdrive/c/me/bat/sjput.bat av_blob.jar

blob_jar: avjar_blob
	cp -f av_blob.jar blob.jar
	cd c:/generatable/java/avbundle && jar uvf /me/work/stjude/java2/blob.jar com net org

isojar:
	(cd Funk; make)
	(cd Trace; make)
	(cd TCGA; make)
	(cd IsoView; make)
	(cd Ace2; make)
	exit
	rm -f isobar.jar
	jar cvfm isobar.jar isobar.manifest Funk/*.class Ace2/*.class TCGA/*.class IsoView/*.class
	cd third_party/unpacked/ && jar uvf ../../av.jar com/ layout/
	/cygdrive/c/me/bat/sjput.bat isobar.jar

avjar_base:
# .ace viewer jar 
	(cd Funk; make)
	(cd Trace; make)
	(cd TCGA; make)
	(cd IsoView; make)
	(cd Ace2; make)
	(cd Ace2; java Ace2.BambinoProperties -increment-build -set-build-date)	
#	exit
	rm -f av.jar
	jar cvfm av.jar av.manifest Trace/*.class Funk/*.class Ace2/*.class Ace2/bambino.properties Ace2/BambinoMuse.obt TCGA/MouseDragScroller.class TCGA/URLLauncher.class TCGA/WebTools.class TCGA/DelayAlerter.class TCGA/URLLauncherDesktop.class TCGA/PopupListener.class TCGA/ControlFrame*.class TCGA/ControlFrameListener.class
	cd third_party/unpacked/ && jar uvf ../../av.jar com/ layout/ net/

avjar:	avjar_base
# .ace viewer jar demo
	echo "rpf before jarsigner"  
	jarsigner -keystore myKeystore av.jar myself
	echo "rpf after jarsigner"  
#	cp -f av.jar onejar/main

#	pscp av.jar edmonson@sshuman.nci.nih.gov:

#bambino: avjar
#	jarsigner -keystore myKeystore av.jar myself

avjar2:	avjar
	/cygdrive/c/me/bat/sjput.bat av.jar


bambino_onejar: avjar
# "onejar" version: works but ONLY WHEN USING MAIN CLASS, CLASSPATH WILL NOT WORK!
	rm -f bambino.jar
	(cd onejar; jar -cvfm ../bambino.jar boot-manifest.mf .)
#	cp -f av.jar bambino.jar
#	cd c:/generatable/java/avbundle && jar uvf /me/work/java2/bambino.jar com net org
	jarsigner -keystore myKeystore bambino.jar myself

bambino_jar: avjar
# single-jar version: database sometimes doesn't work, huh?
# however this version is usable as a CLASSPATH (i.e. to invoke any class within)
	cp -f av.jar bambino.jar
	cd c:/generatable/java/avbundle && jar uvf /me/work/stjude/java2/bambino.jar com net org
	jarsigner -keystore myKeystore bambino.jar myself

avpost:	avjar
#	pscp av.jar edmonson@sshuman.nci.nih.gov:
	pscp av.jar medmonso@erus01:

avpost2:	avjar_base
#	pscp av.jar edmonson@sshuman.nci.nih.gov:
	pscp av.jar medmonso@erus01:lib/

hmpost:
	pscp heatmap.jar edmonson@sshuman.nci.nih.gov:

distclean:
	rm -f *~ *.zip *tmp */*class */*~ README
	rm -fr stubs bak rich z Trace/bak Trace/chromat_dir Trace/old Trace/z
	rm -fr Ace/cool Ace/cool2
	chmod -R a+rx *
	chmod -R go-w *

bundle_third_src:
	rm -fr c:/generatable/java/avbundle
	mkdir c:/generatable/java/avbundle
	(cd c:/generatable/java/avbundle && unzip -o c:/me/work/java2/third_party/sam-1.09.jar)
	(cd c:/generatable/java/avbundle && unzip -o c:/me/work/java2/third_party/mysql-connector-java-5.1.10-bin.jar)

sjbackup:
	perl c:/me/bin/sj_backup
