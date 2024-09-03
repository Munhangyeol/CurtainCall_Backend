package com.example.curtaincall.service;

import com.example.curtaincall.global.exception.PhoneBookNotfoundException;
import com.example.curtaincall.global.exception.UserNotfoundException;
import com.example.curtaincall.repository.PhoneBookRepository;
import com.example.curtaincall.repository.RecentCallLogRepository;
import com.example.curtaincall.repository.UserRepository;
import com.example.curtaincall.domain.PhoneBook;
import com.example.curtaincall.domain.User;
import com.example.curtaincall.dto.*;
import com.example.curtaincall.global.SecretkeyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PhoneBookRepository phoneBookRepository;
    private final SecretkeyManager secretkeyManager;

    public UserService(UserRepository userRepository, PhoneBookRepository phoneBookRepository, RecentCallLogRepository recentCallLogRepository, SecretkeyManager secretkeyManager) {
        this.userRepository = userRepository;
        this.phoneBookRepository = phoneBookRepository;
        this.secretkeyManager = secretkeyManager;
    }

    public void saveUser(RequestUserDTO requestUserDTO) {
        if(userRepository.findByPhoneNumber(encrypt(requestUserDTO.phoneNumber())).isEmpty()) {
            userRepository.save(requestUserDTO.toEntity(encrypt(requestUserDTO.phoneNumber())));
        }
    }

    public void saveUserPhoneBooks(Map<String, List<Contact>> requestPhoneBookDTO) {
        for (String phoneNumber : requestPhoneBookDTO.keySet()) {
            User user = userRepository.findByPhoneNumber(encrypt(phoneNumber)).orElseThrow(
                    PhoneBookNotfoundException::new);

            List<Contact> contacts = requestPhoneBookDTO.get(phoneNumber);
            List<PhoneBook> phoneBooks = contacts.stream()
                    .map(contact -> contact.toEntity(user, encrypt(contact.getPhoneNumber())))
                    .collect(Collectors.toList());

            phoneBookRepository.saveAll(phoneBooks);

            contacts.forEach(contact -> System.out.println("onandoff" + contact.getIsCurtainCallOnAndOff()));
        }
    }

    public void updateUser(RequestUserDTO requestUserDTO, String prePhoneNumber) {
        User user = userRepository.findByPhoneNumber(encrypt(prePhoneNumber)).orElseThrow(
                UserNotfoundException::new
        );

        user.setNickName(requestUserDTO.nickName());
        if(!requestUserDTO.phoneNumber().equals(prePhoneNumber)) {
            user.setPhoneNumber(encrypt(requestUserDTO.phoneNumber()));
        }
        userRepository.save(user);
    }

    public void updatePhoneBook(Map<String, Contact> putRequestDTO, String prePhoneNumber) {
        for (String phoneNumber : putRequestDTO.keySet()) {
            User user = userRepository.findByPhoneNumber(encrypt(phoneNumber)).orElseThrow(
                    UserNotfoundException::new);

            List<PhoneBook> phoneBooks = phoneBookRepository.findByPhoneNumberAndUser(encrypt(prePhoneNumber), user)
                    .orElseThrow(PhoneBookNotfoundException::new);

            for (PhoneBook phoneBook : phoneBooks) {
                Contact contact = putRequestDTO.get(phoneNumber);
                phoneBook.setPhoneNumber(encrypt(contact.getPhoneNumber()));
                phoneBook.setNickName(contact.getName());
                phoneBook.setCurtainCallOnAndOff(contact.getIsCurtainCallOnAndOff());
            }
            phoneBookRepository.saveAll(phoneBooks);
        }
    }

    public ResponseUserDTO findUserByPhoneNumber(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(encrypt(phoneNumber)).orElseThrow(
                UserNotfoundException::new
        );
        return ResponseUserDTO.builder()
                .nickName(user.getNickName())
                .isCurtainCallOnAndOff(true)
                .build();
    }

    public ResponsePhoneBookDTO findPhoneBookByPhoneNumber(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(encrypt(phoneNumber)).orElseThrow(
                UserNotfoundException::new
        );
        List<PhoneBook> phoneBooks = phoneBookRepository.findByUser(user);
        return getResponsePhoneBookDTO(user, phoneBooks);
    }

    public List<Contact> getCurrentUserInfo(String userPhoneNumber, String postPhoneNumber) {
        User user = userRepository.findByPhoneNumber(encrypt(userPhoneNumber)).orElseThrow(
                UserNotfoundException::new
        );

        List<PhoneBook> phoneBooks = phoneBookRepository.findByPhoneNumberAndUser(encrypt(postPhoneNumber), user)
                .orElseThrow(PhoneBookNotfoundException::new);

        return phoneBooks.stream()
                .map(phoneBook -> Contact.builder()
                        .phoneNumber(decrypt(phoneBook.getPhoneNumber()))
                        .name(phoneBook.getNickName())
                        .build())
                .collect(Collectors.toList());
    }

    public ResponsePhoneBookDTO getPhoneBookWithRollback(String userPhoneNumber) {
        User user = userRepository.findByPhoneNumber(encrypt(userPhoneNumber)).orElseThrow(UserNotfoundException::new);
        List<PhoneBook> phoneBooks = phoneBookRepository.findByUser(user);

        phoneBooks.forEach(phoneBook -> phoneBook.setCurtainCallOnAndOff(false));
        phoneBookRepository.saveAll(phoneBooks);

        return getResponsePhoneBookDTO(user, phoneBooks);
    }

    private ResponsePhoneBookDTO getResponsePhoneBookDTO(User user, List<PhoneBook> phoneBooks) {
        Map<String, List<Contact>> contactMap = new HashMap<>();
        List<Contact> contacts = phoneBooks.stream()
                .map(phoneBook -> Contact.builder()
                        .phoneNumber(decrypt(phoneBook.getPhoneNumber()))
                        .name(phoneBook.getNickName())
                        .isCurtainCallOnAndOff(phoneBook.isCurtainCallOnAndOff())
                        .build())
                .collect(Collectors.toList());

        contactMap.put(decrypt(user.getPhoneNumber()), contacts);
        return ResponsePhoneBookDTO.builder().response(contactMap).build();
    }
    private String encrypt(String plainText) {
        return secretkeyManager.encrypt(plainText);
    }

    private String decrypt(String cipherText) {
        return secretkeyManager.decrypt(cipherText);
    }
}
