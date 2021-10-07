⚠️ Warning - This is beta and may break ⚠️

User guide available on [posthog.com/docs/integrate/server/java](https://posthog.com/docs/integrate/server/java).


## Releasing

We're using Sonatype OSSRH to host our open source project binaries.
Docs: https://central.sonatype.org/publish/publish-guide/

#### 1. Get access to be able to deploy

1. First create an account to access JIRA and later the repository manager https://issues.sonatype.org/secure/Signup!default.jspa
2. Create a ticket similar to https://issues.sonatype.org/browse/OSSRH-59076 & get one of the people who already have access to comment on the request with approval (you can ask in #team-platform channel).

#### 2. Prepare your local setup

1. Create an Access User Token here: https://oss.sonatype.org/#profile;User%20Token (same login as for JIRA) we will need it for the settings file below.
2. Create a gpg key and distribute your public key, see docs here: https://central.sonatype.org/publish/requirements/gpg/ (we will need the passphase to be specified in the settings file below).
3. Create a `~/.m2/settings.xml` file with this content (replace the capitalized terms)
```
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>ACCESS_USER_TOKEN_USERNAME</username>
      <password>ACCESS_USER_TOKEN_PASS</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>GPG_PASSPHASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

#### 3. Deploy

1. Change the version in `posthog/pom.xml` accordingly (latest versions can be found here: https://search.maven.org/search?q=com.posthog.java)
2. Run `mvn deploy` in `posthog-java/posthog` folder.

#### 4. Close and release

1. In https://oss.sonatype.org/#stagingRepositories you should see your just pushed files. Click "Close" and check the activity tab to make sure all validations passed (wait and refresh).
2. After all validations passed the Release button will become available to publish the new version.
