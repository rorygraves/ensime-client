package org.ensime.client


//"-XX:MaxPermSize=384m", "-XX:ReservedCodeCacheSize=192m", "-Xms1536m", "-Xmx1536m", "-Xss3m",
case class MemoryConfig(minMemMb: Int=1536,
                        maxMemMb: Int=1536,
                        maxPermSizeMb: Int =384,
                        reservedCodeCacheSizeMb: Int =192,
                        stackSizeMb: Int = 3) {

}
