// Game-agnostic plain Java: who may pass the gate. No game API is ever on this project's classpath.
// Compiled for Java 17 so the same jar can run on Necesse.
tasks.withType<JavaCompile> { options.release.set(17) }
