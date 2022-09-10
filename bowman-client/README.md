# bowman-client

This a copy of [bowman](https://github.com/hdpe/bowman) version 0.9.  Modifications include:
	* Several annotation classes were moved from package `uk.co.blackpepper.bowman.annotation` to a separate artifact named `io.github.da-trunk.naked:sdn-entities`.  This enables entities with those annotations to be dependencies of both client and server.
	* Made several private methods protected so they can be called from `CEClient`.
	* Added code to call `IdClass::setUri` on each bowman-created proxy instances.  Bowman uses URI as the entity's identifier, but spring-data-naked instead stores it in a separate field.
	* Several dependencies were uplifted.  The uplift in HAL dependencies was non-passive.
	* To avoid conflicts, maven coordinates were changed from `me.hdpe.bowman:client` to `io.github.da-trunk.naked:bowman-client`.