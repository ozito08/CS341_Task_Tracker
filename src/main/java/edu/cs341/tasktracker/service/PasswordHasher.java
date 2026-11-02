package edu.cs341.tasktracker.service;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS=120000;
    private static final int KEY_BITS=256;
    private static final SecureRandom RANDOM=new SecureRandom();
    public String hash(char[] password) {
        byte[] salt=new byte[16]; RANDOM.nextBytes(salt);
        byte[] digest=derive(password,salt,ITERATIONS);
        return "pbkdf2-sha256$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+
                Base64.getEncoder().encodeToString(digest);
    }
    public boolean verify(char[] password,String encoded) {
        try {
            String[] parts=encoded.split("\\$");
            if(parts.length!=4 || !"pbkdf2-sha256".equals(parts[0]))return false;
            int count=Integer.parseInt(parts[1]);
            if(count<10000 || count>1000000)return false;
            byte[] salt=Base64.getDecoder().decode(parts[2]);
            byte[] expected=Base64.getDecoder().decode(parts[3]);
            if(salt.length!=16 || expected.length!=32)return false;
            return MessageDigest.isEqual(expected,derive(password,salt,count));
        }catch(RuntimeException e){return false;}
    }
    private byte[] derive(char[] password,byte[] salt,int count) {
        PBEKeySpec spec=new PBEKeySpec(password,salt,count,KEY_BITS);
        try {return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(Exception e) {throw new IllegalStateException("Password hashing is unavailable",e);}
        finally {spec.clearPassword();}
    }
}
