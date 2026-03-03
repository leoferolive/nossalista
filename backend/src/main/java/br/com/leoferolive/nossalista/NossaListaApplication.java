package br.com.leoferolive.nossalista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NossaListaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NossaListaApplication.class, args);
    }

}
