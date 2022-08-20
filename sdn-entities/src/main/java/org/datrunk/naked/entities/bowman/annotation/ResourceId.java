/*
 * Copyright 2016 Black Pepper Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Moved from <a href="https://github.com/hdpe/bowman/blob/main/client/src/main/java/uk/co/blackpepper/bowman/">hdpe/bowman/client</a> 
 * in 2022 to make this type compatible with the artifacts here. 
 * Modifications include changing the groupId and package name.  
 * The original groupId was me.hdpe.bowman.  The original package name was uk.co.blackpepper.bowman.annotation. 
 * This modified file is still licensed
 * under Apache License, Version 2.0, but with copyright 2022 by io.github.da_trunk.
 */
package org.datrunk.naked.entities.bowman.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a property as the entity's {@link java.net.URI} ID.
 * 
 * @author Ryan Pickett
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourceId {
  // marker annotation
}
