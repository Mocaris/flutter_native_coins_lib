package coins.wallet.security;

import android.text.TextUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import kotlin.text.Charsets;

public class AESEncryption {
    // arranged key with hot app
//    public static final String PASSWORD_HOT_APP = "ASD@F&23(4)a@#40";
    // /** 算法/模式/填充 **/
    private static final String CipherMode = "AES/ECB/PKCS5Padding";//CBC
    private static final int KeyLength = 128;//128 bits
    // arranged init vector with hot app
//    private static final String IV_HOT_APP = "A2341234asdfsdg;0''kgsdfik;lkksdfg=];';'SD@F&23(4)a@#4";

    private static SecretKeySpec createKey(String key) {
        byte[] data = null;
        if (key == null) {
            key = "";
        }
        StringBuffer sb = new StringBuffer(KeyLength / 8);
        sb.append(key);
        while (sb.length() < (KeyLength / 8)) {
            sb.append("0");
        }
        if (sb.length() > (KeyLength / 8)) {
            sb.setLength(KeyLength / 8);
        }
        try {
            data = sb.toString().getBytes(Charsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SecretKeySpec(data, "AES");
    }

    private static SecretKeySpec createPasswordKey(String key) {
        byte[] data = null;
        try {
            data = key.getBytes(Charsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SecretKeySpec(data, "AES");
    }

    private static IvParameterSpec createIV(String password) {
        byte[] data = null;
        if (password == null) {
            password = "";
        }
        StringBuffer sb = new StringBuffer((KeyLength / 8));
        sb.append(password);
        while (sb.length() < (KeyLength / 8)) {
            sb.append("0");
        }
        if (sb.length() > (KeyLength / 8)) {
            sb.setLength((KeyLength / 8));
        }

        try {
            data = sb.toString().getBytes(Charsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new IvParameterSpec(data);
    }

    private static byte[] encrypt(byte[] content, String password, String iv) {
        try {
            SecretKeySpec key = createKey(password);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.ENCRYPT_MODE, key, createIV(iv));
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] encrypt(byte[] content, String password) {
        try {
            SecretKeySpec key = createPasswordKey(password);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encrypt string return base58 string of the encrypted
     *
     * @param content
     * @param password
     * @param iv
     * @return
     */
    public static String encrypt(String content, String password, String iv) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        String result = null;
        try {
            byte[] data = encrypt(content.getBytes(Charsets.UTF_8), password, iv);
            byte[] resByte = Base64Java.getMimeEncoder().encode(data);
            result = new String(resByte, Charsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String encrypt(String content, String password) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        String result = null;
        try {
            byte[] data = content.getBytes(Charsets.UTF_8);
            data = encrypt(data, password);
            byte[] resByte = Base64Java.getMimeEncoder().encode(data);
            result = new String(resByte, Charsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static byte[] decrypt(byte[] content, String password, String iv) {
        try {
            SecretKeySpec key = createKey(password);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.DECRYPT_MODE, key, createIV(iv));
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] decrypt(byte[] content, String password) {
        try {
            SecretKeySpec key = createPasswordKey(password);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Decrypt the base58 string of content
     *
     * @param content
     * @param password
     * @param iv
     * @return
     */
    public static String decrypt(String content, String password, String iv) {
        byte[] data = null;
        try {
            data = Base64Java.getMimeDecoder().decode(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = decrypt(data, password, iv);
        if (data == null) {
            return null;
        }
        String result = null;
        try {
            result = new String(data, Charsets.UTF_8);
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String decrypt(String content, String password) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        byte[] data = null;
        try {
            data = Base64Java.getMimeDecoder().decode(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = decrypt(data, password);
        if (data == null) {
            return null;
        }
        String result = null;
        try {
            result = new String(data, Charsets.UTF_8);
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return result;
    }
}
