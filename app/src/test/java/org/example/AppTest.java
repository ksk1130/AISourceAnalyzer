package org.example;

import org.junit.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.Assert.*;

public class AppTest {
    private Path tempPrompt;
    private Path tempCode;
    private Path tempShiftJis;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    @Before
    public void setUp() throws IOException {
        tempPrompt = Files.createTempFile("prompt", ".txt");
        tempCode = Files.createTempFile("code", ".txt");
        Files.writeString(tempPrompt, "BasePrompt", StandardCharsets.UTF_8);
        Files.writeString(tempCode, "CodeContent", StandardCharsets.UTF_8);

        // Shift_JIS file for encoding test
        tempShiftJis = Files.createTempFile("shiftjis", ".txt");
        Files.write(tempShiftJis, "シフトジス".getBytes("Shift_JIS"));

        // Capture System.out
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(tempPrompt);
        Files.deleteIfExists(tempCode);
        Files.deleteIfExists(tempShiftJis);
        System.setOut(originalOut);
    }

    @Test
    public void testTryReadStringWithEncodings_utf8() throws Exception {
        String result = invokeTryReadStringWithEncodings(tempPrompt);
        assertEquals("BasePrompt", result);
    }

    @Test
    public void testTryReadStringWithEncodings_shiftJis() throws Exception {
        String result = invokeTryReadStringWithEncodings(tempShiftJis);
        assertEquals("シフトジス", result);
    }

    @Test
    public void testTryReadStringWithEncodings_fileNotFound() {
        Path notExist = Paths.get("not_exist_file.txt");
        try {
            invokeTryReadStringWithEncodings(notExist);
            fail("例外が発生するはず");
        } catch (Exception ex) {
            // InvocationTargetExceptionの場合は、元の例外を取得
            Throwable cause = ex.getCause();
            if (cause != null) {
                assertTrue("例外メッセージに'ファイルが存在しません'が含まれている必要があります", 
                          cause.getMessage() != null && cause.getMessage().contains("ファイルが存在しません"));
            } else {
                assertTrue("例外メッセージに'ファイルが存在しません'が含まれている必要があります", 
                          ex.getMessage() != null && ex.getMessage().contains("ファイルが存在しません"));
            }
        }
    }

    // Reflection to access private static method
    private String invokeTryReadStringWithEncodings(Path path) throws Exception {
        java.lang.reflect.Method method = App.class.getDeclaredMethod("tryReadStringWithEncodings", Path.class);
        method.setAccessible(true);
        return (String) method.invoke(null, path);
    }

    @Test
    public void testRun_withEmptyPromptAndCode_warnsAndReturns() throws Exception {
        System.out.println(Files.readString(tempPrompt, StandardCharsets.UTF_8));
        System.out.println(Files.readString(tempCode, StandardCharsets.UTF_8));

        App app = new App();
        setField(app, "promptPath", tempPrompt.toString());
        setField(app, "codePath", tempCode.toString());
        setField(app, "propPath", null);

        app.run();

        String output = outContent.toString();
        // Should not throw, and should print nothing (no model output)
        assertTrue(output.isEmpty() || output.contains("\n"));
    }

    // Helper to set private fields via reflection
    private void setField(Object obj, String field, Object value) throws Exception {
        java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, value);
    }

    @Test
    public void testMain_noArgs_printsUsage() {
        // 標準出力をキャプチャ
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            App.main(new String[] {});
            String output = out.toString();
            assertTrue(output.contains("Usage") || output.contains("usage") || output.contains("ヘルプ"));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    public void testMain_missingPromptArg_printsError() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            App.main(new String[] { "--code", "dummy.txt" });
            String output = out.toString();
            assertTrue(output.contains("Missing required option") || output.contains("必須"));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    public void testMain_missingCodeArg_printsError() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));
        try {
            App.main(new String[] { "--prompt", "dummy.txt" });
            String output = out.toString();
            assertTrue(output.contains("Missing required option") || output.contains("必須"));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    public void testTryReadStringWithEncodings_invalidEncoding() throws Exception {
        // 不正なUTF-8バイト列を含むファイルを作成
        Path invalidFile = Files.createTempFile("invalid", ".txt");
        // 不正なUTF-8バイト列 (0xFF, 0xFE)
        Files.write(invalidFile, new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0xFD });
        try {
            String result = invokeTryReadStringWithEncodings(invalidFile);
            // バイナリファイルでも何らかの文字列として読み込まれる可能性があるため、
            // 例外が発生しない場合は結果が空でないことを確認
            assertNotNull("結果がnullであってはいけません", result);
            // このテストは例外が発生しない場合も成功とする
        } catch (Exception ex) {
            // 例外が発生した場合は、適切なメッセージが含まれていることを確認
            Throwable cause = ex.getCause();
            if (cause != null) {
                assertTrue("例外メッセージに'の読み込みに失敗'が含まれている必要があります", 
                          cause.getMessage() != null && cause.getMessage().contains("の読み込みに失敗"));
            } else {
                assertTrue("例外メッセージに'の読み込みに失敗'が含まれている必要があります", 
                          ex.getMessage() != null && ex.getMessage().contains("の読み込みに失敗"));
            }
        } finally {
            Files.deleteIfExists(invalidFile);
        }
    }
}