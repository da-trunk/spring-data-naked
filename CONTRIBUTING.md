# Contributing

## Code Formatting

It is difficult to review a pull request which mixes formatting changes with other changes.  For this reason, this project enforces the [Google Java Style](https://google.github.io/styleguide/javaguide.html) during builds.  It uses the [com.spotify.fmt:fmt-maven-plugin](https://github.com/spotify/fmt-maven-plugin) to reformat files during builds. 

It is recommended to install a plugin to apply this formatting style from your IDE:  
	* If using Eclipse, use the [google-java-format eclipse plugin](https://github.com/google/google-java-format#eclipse).

If you think formatting should be changed, please create a separate PR with updates to the plugins this documentation.  Please do not mix formatting changes with other changes.

## Pull requests

All submissions require review.  We use GitHub pull requests for this purpose.  Your new pull request should trigger a build which will include running all the tests.

## Static Analysis / Reports