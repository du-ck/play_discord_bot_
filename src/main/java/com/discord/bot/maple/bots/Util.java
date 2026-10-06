package com.discord.bot.maple.bots;

import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class Util {

    /**
     * 리소스는 jar 내부(src/main/resources)에 들어있다.
     * jar 안의 항목은 실제 파일이 아니므로 File 로는 접근할 수 없고 스트림으로만 읽는다.
     */
    public InputStream stream(String fileName) {
        InputStream in = Util.class.getClassLoader().getResourceAsStream(fileName);
        if (in == null) {
            throw new IllegalArgumentException("Resource not found on classpath: " + fileName);
        }
        return in;
    }

    /** 디스코드 업로드용. 전송이 끝나면 JDA 가 스트림을 닫는다. */
    public FileUpload upload(String fileName) {
        return FileUpload.fromData(stream(fileName), fileName);
    }

    /**
     * HHmm 형태의 문자열값을
     * 앞 2자리 HH → 00~23
     * 뒤 2자리 mm → 00~59
     * 유효성검사 로직
     */
    public static boolean isValidHHmm(String input) {
        if (input == null || input.length() != 4 || !input.matches("\\d{4}")) {
            return false;
        }

        int hh = Integer.parseInt(input.substring(0, 2));
        int mm = Integer.parseInt(input.substring(2, 4));

        return hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59;
    }
}
