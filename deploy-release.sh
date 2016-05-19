# This shell script is used to automate the process of deploying a stable
# release version (i.e. one in which the version does not end in -SNAPSHOT).
# Further discussion may be found on the RTW wiki.

# Save a copy of pom.xml, which we will be temporarily modifying
cp pom.xml pom.xml.backup

# Switch dependences over to release versions.  This ensures that all stable
# releases build against other stable releases.

# We may want to switch to a less hamfisted way of doing this, like looking
# only in the dependency section and picking out only things in edu.cmu.ml.rtw
# or that are somehow marked as being our own internal dependencies.  But
# we'll start quick and simple.

# Note that this conveniently also removes -SNAPSHOT from the version of this
# project as well.
sed -i 's/-SNAPSHOT<\/version>/<\/version>/' pom.xml

# Rebuild and deploy.  This will fail to deploy if the build fails.
mvn clean deploy -U

# Regardless of what happened, restore original pom.xml
mv pom.xml.backup pom.xml
