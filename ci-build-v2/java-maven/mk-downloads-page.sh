set -e

echo "TODO: TO BE UPDATED"
exit 1


wget -O /tmp/download-page-utils.sh \
  https://raw.githubusercontent.com/Rothamsted/knetminer-common/master/download-page/download-page-utils.sh

. /tmp/download-page-utils.sh

wdir="`pwd`"
cd "`dirname "$0"`"
mydir="`pwd`"
cd "$wdir"


# Gets all the download links by chaining multiple invocations of make_doc()/Nexus-API
#

# TODO: The 3.0 specified below is to obtain the right release, rather than 3.0-RC
# I've filed a bug to Sonatype about this (https://issues.sonatype.org/browse/NEXUS-24220). 
# We need a more stable solution, like  results filtering.
#

cat "$mydir/Downloads-template.md" \
| make_doc maven-snapshots uk.ac.rothamsted.kg rdf2neo-cli '' zip rdf2neoSnap \
| make_doc maven-releases uk.ac.rothamsted.kg rdf2neo-cli '' zip rdf2neoRel \
| make_doc maven-snapshots uk.ac.rothamsted.kg rdf2graphml-cli '' zip rdf2graphmlSnap \
| make_doc maven-releases uk.ac.rothamsted.kg rdf2graphml-cli '' zip rdf2graphmlRel
