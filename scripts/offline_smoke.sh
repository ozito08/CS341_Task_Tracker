#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
find "$ROOT/src/main/java" -name '*.java' -print0 | xargs -0 javac --release 11 -d "$OUT"
cat > "$OUT/HashSmoke.java" <<'JAVA'
import edu.cs341.tasktracker.service.PasswordHasher;
public class HashSmoke {
 public static void main(String[] a) {
  PasswordHasher h=new PasswordHasher(); String p=h.hash("ExamplePass123".toCharArray());
  if(!h.verify("ExamplePass123".toCharArray(),p)||h.verify("bad".toCharArray(),p))
   throw new AssertionError("PasswordHasher smoke test failed");
  System.out.println("PASS: Java 11 compilation and PBKDF2 smoke test");
 }
}
JAVA
javac --release 11 -cp "$OUT" -d "$OUT" "$OUT/HashSmoke.java"
java -cp "$OUT" HashSmoke
