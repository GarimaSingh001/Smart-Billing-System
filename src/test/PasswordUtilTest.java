package test;

import static test.TestSupport.check;
import static test.TestSupport.expectThrows;
import static test.TestSupport.section;

import util.PasswordUtil;

/** Tests SHA-256 hashing and the password policy. Run: java -cp out test.PasswordUtilTest */
public class PasswordUtilTest {

    /** Runs all checks. */
    public static void run() {
        section("PasswordUtilTest");
        check("known SHA-256 vector for 'abc'", PasswordUtil.hash("abc")
                .equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"));
        check("hash is 64 lowercase hex characters", PasswordUtil.hash("admin123").matches("[0-9a-f]{64}"));
        check("hash is deterministic", PasswordUtil.hash("x1y2z3").equals(PasswordUtil.hash("x1y2z3")));
        check("different passwords give different hashes", !PasswordUtil.hash("admin123").equals(PasswordUtil.hash("admin124")));
        check("hash never equals the plain text", !PasswordUtil.hash("admin123").equals("admin123"));
        check("verify accepts the right password", PasswordUtil.verify("cash123", PasswordUtil.hash("cash123")));
        check("verify rejects a wrong password", !PasswordUtil.verify("cash124", PasswordUtil.hash("cash123")));
        check("verify rejects null input", !PasswordUtil.verify(null, PasswordUtil.hash("cash123")));
        check("policy accepts 6 characters", PasswordUtil.isAcceptable("abc123"));
        check("policy rejects 5 characters", !PasswordUtil.isAcceptable("abc12"));
        check("policy rejects blank", !PasswordUtil.isAcceptable("      "));
        expectThrows("hash(null) is rejected", IllegalArgumentException.class, new TestSupport.Block() {
            @Override
            public void run() throws Exception {
                PasswordUtil.hash(null);
            }
        });
    }

    /** Standalone entry point. */
    public static void main(String[] args) {
        run();
        TestSupport.finish();
    }
}
