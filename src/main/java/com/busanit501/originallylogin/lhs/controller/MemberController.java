package com.busanit501.originallylogin.lhs.controller;


import com.busanit501.springproject3.lhs.Util.JwtUtil;
import com.busanit501.springproject3.lhs.dto.MemberJoinDTO;
import com.busanit501.springproject3.lhs.dto.upload.UploadResultDTO;
import com.busanit501.springproject3.lhs.service.MemberService;
import com.busanit501.springproject3.lhs.service.MemberServiceImpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/member")
@Log4j2
@RequiredArgsConstructor
public class MemberController {

    @Autowired
    private JwtUtil jwtUtil;

    private final MemberServiceImpl memberServiceImpl;
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String restAPIKEY;

    private final MemberService memberService;


    //카카오 소셜 로그 아웃
    // restAPIKEY -> application.properties 에 등록된  REST API key
    // logout_redirect_uri : 로그 아웃 후 이동할 페이지. 카카오 개발자 사이트에서,
    // 카카오 로그인 -> 고급 -> 로그아웃 리다이렉트 등록하기.
    // 공식 문서 : https://developers.kakao.com/docs/latest/ko/kakaologin/common#logout-with-kakao-account
    // 로그 아웃시, 2번째 클릭 확인 , 완전 계정 로그 아웃
    // 로그 아웃시, 1번째 클릭 확인 , 이 서비스 로그 아웃, 다시 카카오 로그인시 로그인이 됨
    @GetMapping("/kakaoLogout")
    public String kakaoLogout(String error, String logout,
                         RedirectAttributes redirectAttributes) {
      return "redirect:https://kauth.kakao.com/oauth/logout?client_id="+restAPIKEY+"&logout_redirect_uri=http://localhost:8080/member/login";

    }

    // 중복아이디 검사, 메서드에서, 레스트 형식으로 checkID
    @GetMapping("/checkID/{mid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getResourceByMid(@PathVariable String mid) {
            boolean result = memberService.checkMid(mid);
        Map<String, Object> resultMap = new HashMap<>();
        if (result) {
            resultMap.put("result", true);
            return ResponseEntity.ok(resultMap);
        } else {
            resultMap.put("result", false);
            return ResponseEntity.ok(resultMap);
        }
    }

    @GetMapping("/login")
    public void loginGet(String error, String logout,
                         RedirectAttributes redirectAttributes) {
        log.info("loginGet===================");
        log.info("logoutTest ===================" + logout);

        if (logout != null) {
            log.info("logoutTest user ====================");
        }
        if (error != null) {
            log.info("logoutTest error ====================" + error);
            redirectAttributes.addFlashAttribute(
                    "error", "");
        }

    }

    // 로그인 처리
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 실제 로그인 검증 로직은 UserService에 구현되어 있어야 합니다.
        if (memberServiceImpl.authenticateUser(request.getMid(), request.getPassword())) {
            String accessToken = jwtUtil.generateAccessToken(request.getMid());
            String refreshToken = jwtUtil.generateRefreshToken(request.getMid());
            return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken));
        } else {
            return ResponseEntity.status(401).body(null); // 인증 실패 시 401 응답
        }
    }
//    로그인 부분(로컬 저장소)
//    @PostMapping("/login")
//    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
//        Map<String, String> resultMap = new HashMap<>();
//
//        if (memberServiceImpl.authenticateUser(request.getMid(), request.getPassword())) {
//            String accessToken = jwtUtil.generateAccessToken(request.getMid());
//            String refreshToken = jwtUtil.generateRefreshToken(request.getMid());
//            resultMap.put("accessToken", accessToken);
//            resultMap.put("refreshToken", refreshToken);
//            return ResponseEntity.ok(resultMap);
//        } else {
//            resultMap.put("error", "Invalid credentials");
//            return ResponseEntity.status(401).body(resultMap); // 인증 실패 시 401 응답
//        }
//    }
    // 로그인 엔드포인트 추가
//    @PostMapping("/login")
//    public ResponseEntity<Map<String, String>> loginPost(@RequestBody MemberJoinDTO memberJoinDTO) {
//        log.info("loginPost====================");
//        Map<String, String> resultMap = new HashMap<>();
//        try {
//            String token = memberService.login(memberJoinDTO.getMid(), memberJoinDTO.getMpw());
//            resultMap.put("token", token);
//            return ResponseEntity.ok(resultMap);
//        } catch (Exception e) {
//            resultMap.put("error", "Invalid credentials");
//            return ResponseEntity.badRequest().body(resultMap);
//        }
//    }

    // 로그인 폼은 있지만, 로그인을 처리하는 로직처리가 없어요?
    // 누가 처리? 스프링의 시큐리티가 알아서 처리함.
    // 자동로그인, remember-me 이름으로 서버에 와요.


    //회원수정 폼
    // 1) 소셜로 로그인 했을 경우
    // 2) 직접 로그인 했을 경우.
    @GetMapping("/update")
    public void updateGet(@AuthenticationPrincipal UserDetails user, Model model) {
        log.info("updateGet====================");
        log.info("updateGet : " + user);
        // 로그인 여부에 따라, 로그 아웃 표시하기.
        model.addAttribute("user", user);
    }

    // 회원 수정 로직 처리, 1) 소셜 로그인 경우, 패스워드만 변경
    // 2) 직접 로그인시, 패스워드, 이메일, 프로필 이미지도 변경.
    @PostMapping("/update")
    public String updatePost(MemberJoinDTO memberJoinDTO, @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                             RedirectAttributes redirectAttributes) {
        log.info("updatePost====================");
        //현재 패스워드만 들어가 있음. 보이지 않게 mid, email 정보도 넘어옴.
        log.info("memberJoinDTO = " + memberJoinDTO);
//        log.info("memberJoinDTO = 2 profileImage " + profileImage.getOriginalFilename());

        // 프로필 이미지가 있을 경우, 이미지를 먼저 업로드 함.
        String resultProfileImage = "";
        if(profileImage != null && !profileImage.isEmpty()) {
            UploadResultDTO uploadResultDTO = memberService.uploadProfileImage(profileImage);
            // resultProfileImage -> s_uuid_파일명
            resultProfileImage = uploadResultDTO.getLink();
            log.info("memberJoinDTO 이미지가 있는 경우 : " + resultProfileImage);

//        memberJoinDTO.addProfileImage(resultProfileImage);
            String[] arr = resultProfileImage.split("_");
            memberJoinDTO.setUuid(arr[1]);
            memberJoinDTO.setFileName(arr[2]);
        }

        log.info("memberJoinDTO = 3 프로필 이미지 있는 경우  " + memberJoinDTO);
        // 회원 수정 로직 처리 없음.
        try {
            //memberService.join(memberJoinDTO);
            memberService.update(memberJoinDTO);
        } catch (MemberService.MidExistException e) {
            redirectAttributes.addFlashAttribute("error", "아이디 중복입니다");
            return "redirect:/member/update";
        }
        // 회원 수정 성공시
        redirectAttributes.addFlashAttribute("result","회원수정 성공");
        return "redirect:/member/logout";
    }


    @PostMapping("/updateSocial")
    public String updateSocial(String mid, String mpw, RedirectAttributes redirectAttributes) {
        log.info("updateSocial====================");
        //현재 패스워드만 들어가 있음. 보이지 않게 mid, email 정보도 넘어옴.

//        log.info("memberJoinDTO = 2 profileImage " + profileImage.getOriginalFilename());
        log.info("mid : " + mid + ", mpw : " + mpw);

        // 회원 수정 로직 처리 없음.
        try {
            //memberService.join(memberJoinDTO);
            memberService.updateSocial(mid,mpw);
        } catch (MemberService.MidExistException e) {
            redirectAttributes.addFlashAttribute("error", "아이디 중복입니다");
            return "redirect:/member/update";
        }
        // 회원 수정 성공시
        redirectAttributes.addFlashAttribute("result","회원수정 성공");
        return "redirect:/board/list";
    }


    // 회원가입 폼
    @GetMapping("/join")
    public void joinGet() {
        log.info("joinGet====================");
    }

//    // 회원가입 엔드포인트 추가 (JWT 포함)
//    @PostMapping("/join")
//    public ResponseEntity<Map<String, String>> joinPost(MemberJoinDTO memberJoinDTO, @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
//                                                        RedirectAttributes redirectAttributes) {
//        log.info("joinPost====================");
//        String resultProfileImage = "";
//        Map<String, String> resultMap = new HashMap<>();
//
//        if (profileImage != null && !profileImage.isEmpty()) {
//            UploadResultDTO uploadResultDTO = memberService.uploadProfileImage(profileImage);
//            resultProfileImage = uploadResultDTO.getLink();
//            String[] arr = resultProfileImage.split("_");
//            memberJoinDTO.setUuid(arr[1]);
//            memberJoinDTO.setFileName(arr[2]);
//        }
//
//        try {
//            memberService.join(memberJoinDTO);
//            String token = jwtUtil.generateToken(memberJoinDTO.getMid());
//            resultMap.put("token", token);
//            return ResponseEntity.ok(resultMap);
//        } catch (MemberService.MidExistException e) {
//            resultMap.put("error", "아이디 중복입니다");
//            return ResponseEntity.badRequest().body(resultMap);
//        }
//    }
//}

    // 회원 가입 로직 처리
    @PostMapping("/join")
    public String joinPost(MemberJoinDTO memberJoinDTO, @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                           RedirectAttributes redirectAttributes) {
        log.info("joinPost====================");
        log.info("memberJoinDTO = " + memberJoinDTO);
//        log.info("memberJoinDTO = 2 profileImage " + profileImage.getOriginalFilename());

        // 프로필 이미지가 있을 경우, 이미지를 먼저 업로드 함.
        String resultProfileImage = "";
        if(profileImage != null && !profileImage.isEmpty()) {
            UploadResultDTO uploadResultDTO = memberService.uploadProfileImage(profileImage);
            // resultProfileImage -> s_uuid_파일명
            resultProfileImage = uploadResultDTO.getLink();
            log.info("memberJoinDTO 이미지가 있는 경우 : " + resultProfileImage);

//        memberJoinDTO.addProfileImage(resultProfileImage);
            String[] arr = resultProfileImage.split("_");
            memberJoinDTO.setUuid(arr[1]);
            memberJoinDTO.setFileName(arr[2]);
        }

        log.info("memberJoinDTO = 3 프로필 이미지 있는 경우  " + memberJoinDTO);
        // 회원 가입 로직 처리 없음.
        try {
            memberService.join(memberJoinDTO);
        } catch (MemberService.MidExistException e) {
            redirectAttributes.addFlashAttribute("error", "아이디 중복입니다");
            return "redirect:/member/join";
        }
        // 회원 가입 성공시
        redirectAttributes.addFlashAttribute("result","회원가입 성공");
        return "redirect:/member/login";
    }
    // 회원가입 후 JWT 반환(로컬저장소)
//    @PostMapping("/join")
//    public ResponseEntity<Map<String, String>> joinPost(@RequestBody MemberJoinDTO memberJoinDTO, @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
//        log.info("joinPost====================");
//        String resultProfileImage = "";
//        Map<String, String> resultMap = new HashMap<>();
//
//        if (profileImage != null && !profileImage.isEmpty()) {
//            UploadResultDTO uploadResultDTO = memberService.uploadProfileImage(profileImage);
//            resultProfileImage = uploadResultDTO.getLink();
//            String[] arr = resultProfileImage.split("_");
//            memberJoinDTO.setUuid(arr[1]);
//            memberJoinDTO.setFileName(arr[2]);
//        }
//
//        try {
//            String accessToken = memberService.join(memberJoinDTO); // 토큰 생성 후 반환
//            String refreshToken = memberService.generateRefreshToken(memberJoinDTO.getMid());
//            resultMap.put("accessToken", accessToken);
//            resultMap.put("refreshToken", refreshToken);
//            return ResponseEntity.ok(resultMap); // 클라이언트에 토큰 반환
//        } catch (MemberService.MidExistException e) {
//            resultMap.put("error", "아이디 중복입니다");
//            return ResponseEntity.badRequest().body(resultMap);
//        }
//    }
}

@Data
class LoginResponse {
    private String accessToken;
    private String refreshToken;

    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
}

@Data
class LoginRequest {
    private String mid;
    private String password;

    // Getters and Setters
}

