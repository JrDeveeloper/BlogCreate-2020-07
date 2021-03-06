package com.sbs.java.blog.controller;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sbs.java.blog.dto.Member;
import com.sbs.java.blog.util.Util;

public class MemberController extends Controller {

	public MemberController(Connection dbConn, String actionMethodName, HttpServletRequest req,
			HttpServletResponse resp) {
		super(dbConn, actionMethodName, req, resp);
	}

	public String doAction() {
		switch (actionMethodName) {
		case "join":
			return actionJoin();
		case "doJoin":
			return actionDoJoin();
		case "login":
			return actionLogin();
		case "doLogin":
			return actionDoLogin();
		case "doLogout":
			return actionDoLogout();
		case "getLoginIdDup":
			return actionGetLoginIdDup();
		case "passwordForPrivate":
			return actionPasswordForPrivate();
		case "doPasswordForPrivate":
			return actionDoPasswordForPrivate();
		case "modifyPrivate":
			return actionModifyPrivate();
		case "doModifyPrivate":
			return actionDoModifyPrivate();
		case "findAccount":
			return actionFindAccount();
		case "doFindLoginId":
			return actionDoFindLoginId();
		case "doFindLoginPw":
			return actionDoFindLoginPw();
		}

		return "";
	}

	private String actionDoFindLoginPw() {
		String loginId = Util.getString(req, "loginId");
		String email = Util.getString(req, "email");

		Member member = memberService.getMemberByLoginId(loginId);

		if (member == null || member.getEmail().equals(email) == false) {
			req.setAttribute("jsAlertMsg", "일치하는 회원이 없습니다.");
			req.setAttribute("jsHistoryBack", true);
			return "common/data.jsp";
		}

		memberService.notifyTempLoginPw(member);
		req.setAttribute("jsAlertMsg", "메일로 임시패스워드가 발송되었습니다.");
		req.setAttribute("redirectUri", "../member/login");

		return "common/data.jsp";
	}

	private String actionDoFindLoginId() {
		String name = Util.getString(req, "name");
		String email = Util.getString(req, "email");

		Member member = memberService.getMemberByNameAndEmail(name, email);

		if (member == null) {
			req.setAttribute("jsAlertMsg", "일치하는 회원이 없습니다.");
			req.setAttribute("jsHistoryBack", true);
			return "common/data.jsp";
		}

		req.setAttribute("jsAlertMsg", "일치하는 회원을 찾았습니다.\\n아이디 : " + member.getLoginId());
		req.setAttribute("jsHistoryBack", true);
		return "common/data.jsp";
	}

	private String actionFindAccount() {
		return "member/findAccount.jsp";
	}

	private String actionDoModifyPrivate() {
		int loginedMemberId = (int) req.getAttribute("loginedMemberId");
		String authCode = req.getParameter("authCode");

		if (memberService.isValidModifyPrivateAuthCode(loginedMemberId, authCode) == false) {
			return String.format(
					"html:<script> alert('비밀번호를 다시 체크해주세요.'); location.replace('../member/passwordForPrivate'); </script>");
		}

		String loginPw = req.getParameter("loginPwReal");

		memberService.modify(loginedMemberId, loginPw);
		Member loginedMember = (Member) req.getAttribute("loginedMember");
		loginedMember.setLoginPw(loginPw); // 크게 의미는 없지만, 의미론적인 면에서 해야 하는

		return String.format("html:<script> alert('개인정보가 수정되었습니다.'); location.replace('../home/main'); </script>");
	}

	private String actionModifyPrivate() {
		int loginedMemberId = (int) req.getAttribute("loginedMemberId");

		String authCode = req.getParameter("authCode");
		if (memberService.isValidModifyPrivateAuthCode(loginedMemberId, authCode) == false) {
			return String.format(
					"html:<script> alert('비밀번호를 다시 체크해주세요.'); location.replace('../member/passwordForPrivate'); </script>");
		}

		return "member/modifyPrivate.jsp";
	}

	private String actionDoPasswordForPrivate() {
		String loginPw = req.getParameter("loginPwReal");

		Member loginedMember = (Member) req.getAttribute("loginedMember");
		int loginedMemberId = loginedMember.getId();

		if (loginedMember.getLoginPw().equals(loginPw)) {
			String authCode = memberService.genModifyPrivateAuthCode(loginedMemberId);

			return String
					.format("html:<script> location.replace('modifyPrivate?authCode=" + authCode + "'); </script>");
		}

		return String.format("html:<script> alert('비밀번호를 다시 입력해주세요.'); history.back(); </script>");
	}

	private String actionPasswordForPrivate() {
		return "member/passwordForPrivate.jsp";
	}

	private String actionGetLoginIdDup() {
		String loginId = req.getParameter("loginId");

		boolean isJoinableLoginId = memberService.isJoinableLoginId(loginId);

		if (isJoinableLoginId) {
			return "json:{\"msg\":\"사용할 수 있는 아이디 입니다.\", \"resultCode\": \"S-1\", \"loginId\":\"" + loginId + "\"}";
		} else {
			return "json:{\"msg\":\"사용할 수 없는 아이디 입니다.\", \"resultCode\": \"F-1\", \"loginId\":\"" + loginId + "\"}";
		}
	}

	private String actionDoLogin() {
		String loginId = req.getParameter("loginId");
		String loginPw = req.getParameter("loginPwReal");

		int loginedMemberId = memberService.getMemberIdByLoginIdAndLoginPw(loginId, loginPw);

		if (loginedMemberId == -1) {
			return String.format("html:<script> alert('일치하는 정보가 없습니다.'); history.back(); </script>");
		}

		session.setAttribute("loginedMemberId", loginedMemberId);
		boolean isNeedToChangePasswordForTemp = memberService.isNeedToChangePasswordForTemp(loginedMemberId);

		String redirectUri = Util.getString(req, "redirectUri", "../home/main");
		
		req.setAttribute("jsAlertMsg", "로그인 되었습니다.");
		
		if ( isNeedToChangePasswordForTemp ) {
			req.setAttribute("jsAlertMsg2", "현재 임시패스워드를 사용중입니다. 비밀번호를 변경해주세요.");	
		}
		
		req.setAttribute("redirectUri", redirectUri);
		return "common/data.jsp";
	}

	private String actionLogin() {
		return "member/login.jsp";
	}

	private String actionDoLogout() {
		session.removeAttribute("loginedMemberId");

		String redirectUri = Util.getString(req, "redirectUri", "../home/main");

		return String.format("html:<script> alert('로그아웃 되었습니다.'); location.replace('" + redirectUri + "'); </script>");
	}

	private String actionDoJoin() {

		String loginId = req.getParameter("loginId");
		String loginPw = req.getParameter("loginPwReal");
		String name = req.getParameter("name");
		String nickname = req.getParameter("nickname");
		String email = req.getParameter("email");

		boolean isJoinableLoginId = memberService.isJoinableLoginId(loginId);

		if (isJoinableLoginId == false) {
			return String.format("html:<script> alert('%s(은)는 이미 사용중인 아이디 입니다.'); history.back(); </script>", loginId);
		}

		boolean isJoinableNickname = memberService.isJoinableNickname(nickname);

		if (isJoinableNickname == false) {
			return String.format("html:<script> alert('%s(은)는 이미 사용중인 닉네임 입니다.'); history.back(); </script>", nickname);
		}

		boolean isJoinableEmail = memberService.isJoinableEmail(email);

		if (isJoinableEmail == false) {
			return String.format("html:<script> alert('%s(은)는 이미 사용중인 이메일 입니다.'); history.back(); </script>", email);
		}

		memberService.join(loginId, loginPw, name, nickname, email);

		return String.format("html:<script> alert('%s님 환영합니다.'); location.replace('../home/main'); </script>", name);
	}

	private String actionJoin() {
		return "member/join.jsp";
	}

	@Override
	public String getControllerName() {
		return "member";
	}
}