# Continuous Integration Scripts for RDF2PG

Scripts in the hereby `ci-build-v2/` directory are used to manage both the KnetMiner CI builds and to make new releases. The scripts are based on the [KnetMiner CI][IN10] project, which is explained in [this presentation][IN20].

Scripts from the project are downloaded locally and launched by a dedicatd [GitHub Action workflow][IN30].

[IN10]: https://github.com/KnetMiner/knetminer-ci
[IN20]: https://www.slideshare.net/mbrandizi/continuos-integration-knetminer
[IN30]: ../.github/workflows/build.yml


## Release variant

The same scripts can be used for releasing the KnetMiner API. As per the parent [KnetMiner CI][IN10] project, this can be done by [triggering the build workflow manually][RL10] (Using the 'Run Workflow' button) and passing a release version plus the version for the next Maven snapshot, for instance, `1.0` and `1.0.1-SNAPSHOT`. The applied policy is that the workflow first tags Maven and the git code with the release version, builds everything against that version and then changes the Maven POMs again to move them to the next snapshot/development version, with the idea that any new changes belong to the next release.

[RL10]: https://github.com/KnetMiner/rdf2pg/actions/workflows/build.yml
