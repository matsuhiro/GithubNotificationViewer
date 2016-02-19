package net.matsuhiro.github.notificationviewer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Base64;

import java.security.Key;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ScrambleUtil {
    public static byte[] getScrambleDigest(Context ctx) {
        String signature = null;
        PackageInfo pkgInfo;
        try {
            pkgInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(pkgInfo.signatures[0].toByteArray());
            byte[] digest_byte = digest.digest();
            signature = Base64.encodeToString(digest_byte, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
        byte[] result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(signature.getBytes());
           result = digest.digest();
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    public static String decrypt(byte[] key, String src) {
        if (key == null || TextUtils.isEmpty(src)) {
            return "";
        }
        String decrypted = "";
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivspec = new IvParameterSpec(key);
            Key skey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, skey, ivspec);
            byte[] src_byte = Base64.decode(src, Base64.DEFAULT);
            byte[] decryptedText = cipher.doFinal(src_byte);
            decrypted = new String(decryptedText, "UTF-8");
        } catch (Exception e) {
            return "";
        }
        return decrypted;
    }

    public static String encrypt(byte[] key, String src) {
        if (key == null || TextUtils.isEmpty(src)) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivspec = new IvParameterSpec(key);
            Key skey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, skey, ivspec);
            byte[] src_byte = src.getBytes("UTF-8");
            byte[] encryptedText = cipher.doFinal(src_byte);
            return Base64.encodeToString(encryptedText, Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }
}
