package com.lld.auth;

import com.lld.auth.security.PasswordEncoder.CustomPasswordEncoderFactories;
import com.lld.auth.security.PasswordEncoder.RandomPasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthApplicationTests {


    @Test
    void test() {
        RandomPasswordEncoder randomPasswordEncoder1 = CustomPasswordEncoderFactories.createRandomPasswordEncoder();
        System.out.println(randomPasswordEncoder1.encode("123465"));;
        System.out.println(randomPasswordEncoder1.encode("123465"));;
        System.out.println(randomPasswordEncoder1.encode("123465"));;
    }

}
