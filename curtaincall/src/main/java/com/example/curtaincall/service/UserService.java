package com.example.curtaincall.service;

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
        userRepository.save(requestUserDTO.toEntity(secretkeyManager.encrypt(requestUserDTO.phoneNumber())));
    }
    public void saveUserPhoneBooks(Map<String, List<Contact>> requestPhoneBookDTO){
        for (String stringListEntry : requestPhoneBookDTO.keySet()) {
            User user = userRepository.findByPhoneNumber(secretkeyManager.encrypt(stringListEntry)).orElseThrow(
                    () -> new RuntimeException("This phonenumber is not in this repostiory"));
            requestPhoneBookDTO.get(stringListEntry).forEach(
                    contact -> phoneBookRepository.save(contact.toEntity(user,
                            secretkeyManager.encrypt(contact.getPhoneNumber())
                    ))//이거 그냥 saveall로 처리하는 방향으로 리펙토링하는걸로.
            );
        }
    }
    public void updateUser(RequestUserDTO requestUserDTO){
        Optional<User> user = userRepository.findByPhoneNumber(secretkeyManager.encrypt(requestUserDTO.phoneNumber()));
        if(user.isPresent()){
            User notNullUser = user.get();
            notNullUser.setNickName(requestUserDTO.nickName());
            notNullUser.setPhoneNumber(secretkeyManager.encrypt(requestUserDTO.phoneNumber()));
            userRepository.save(notNullUser);
        }
    }
    public void updatePhoneBook(Map<String, Contact> putRequestDTO, String prePhoneNumber) {
        for (String stringListEntry : putRequestDTO.keySet()) {
            User user = userRepository.findByPhoneNumber(secretkeyManager.encrypt(stringListEntry)).orElseThrow(
                    () -> new RuntimeException("This phonenumber is not in this repostiory"));
            Contact contact = putRequestDTO.get(stringListEntry);
            List<PhoneBook> phoneBooks = phoneBookRepository.findByPhoneNumberAndUser(prePhoneNumber, user).
                    orElseThrow(()-> new RuntimeException("This phoneNumber and user is not in this repository"));
            if(phoneBooks.isEmpty()){
                throw new RuntimeException("This phoneNumber and user is not in this repository");
            }
            for (PhoneBook phoneBook : phoneBooks) {
                phoneBook.setPhoneNumber(putRequestDTO.get(stringListEntry).getPhoneNumber());
                phoneBook.setNickName(putRequestDTO.get(stringListEntry).getPhoneNumber());
            }
            phoneBookRepository.saveAll(phoneBooks);
        }
    }
    public ResponseUserDTO findUserByPhoneNumber(String phoneNumber){
        User user = userRepository.findByPhoneNumber(secretkeyManager.encrypt(phoneNumber)).orElseThrow(
                () -> new RuntimeException("This user is not in this repository")
        );
        return ResponseUserDTO.builder().nickName(user.getNickName())
                .isCurtainCallOnAndOff(true)
                .build();
    }
    public ResponsePhoneBookDTO findPhoneBookByPhoneNumber(String phoneNumber){
        User user=userRepository.findByPhoneNumber(secretkeyManager.encrypt(phoneNumber)).orElseThrow(
                () -> new RuntimeException("This user is not in this repository")
        );
        List<PhoneBook> phoneBooks= phoneBookRepository.findByUser(user);
        Map<String, List<Contact>> contactMap = new HashMap<>();
        List<Contact> contacts = new ArrayList<>();
        phoneBooks.forEach(
                phoneBook -> contacts.add(Contact.builder()
                        .phoneNumber(secretkeyManager.decrypt(phoneBook.getPhoneNumber()))
                        .name(phoneBook.getNickName())
                        .build())

        );
        contactMap.put(secretkeyManager.decrypt(user.getPhoneNumber()), contacts);
        return ResponsePhoneBookDTO.builder().response(contactMap).build();
    }
    public List<Contact> getCurrentUserInfo(String userPhoneNumber,String postPhoneNumber){
        User user = userRepository.findByPhoneNumber(secretkeyManager.encrypt(userPhoneNumber)).orElseThrow(
                ()->new RuntimeException("This user is not in this repository")
        );
        List<PhoneBook> phoneBooks = phoneBookRepository.findByPhoneNumberAndUser(secretkeyManager.encrypt(postPhoneNumber), user)
                .orElseThrow(() -> new RuntimeException("This phoneNumber and user is not in this repository"));
        if(phoneBooks.isEmpty()){
            throw new RuntimeException("This phoneNumber and user is not in this repository");
        }
        return phoneBooks.stream().map(phoneBook -> Contact.builder().
                phoneNumber(secretkeyManager.decrypt(phoneBook.getPhoneNumber())).name(phoneBook.getNickName()
                        ).build()).collect(Collectors.toList());

    }


}
