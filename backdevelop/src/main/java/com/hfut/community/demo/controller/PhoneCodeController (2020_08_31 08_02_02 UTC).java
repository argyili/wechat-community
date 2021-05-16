package com.hfut.community.demo.controller;
import com.hfut.community.demo.domain.Admin;
import com.hfut.community.demo.domain.Phone;
import com.hfut.community.demo.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 */
@Controller
public class PhoneCodeController {
    /**
     */
    @Autowired
    private AdminService adminService;
    /**
     */
    public static final String DEF_CHATSET = "UTF-8";
    /**
     */
    public static final int DEF_CONN_TIMEOUT = 30000;
    /**
     */
    public static final int DEF_READ_TIMEOUT = 30000;
    /**
     */
    public static final int SIX = 6;
    /**
     */
    public static final int TEN = 10;
    /**
     */
    public static final int PHONENUM = 11;
    /**
     */
    private static String userAgent =
            "Mozilla/5.0 (Windows NT 6.1) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/29.0.1547.66 "
            + "Safari/537.36";
    /**
     * 获取验证码
     * @param phone Phone
     * @param model Model
     * @param session HttpSession
     * @return String
     */
    @RequestMapping(value = "/phone", method = RequestMethod.GET)
    public String mobileQuery(@ModelAttribute(value = "Phone") Phone phone,
                              Model model, HttpSession session) {
        if (phone.getPhoneCode() == null) {
            return "phone";
        }
        if ("".equals(phone.getPhoneCode())) {
            model.addAttribute("message", "手机号不能为空");
            return "phone";
        }
        if (phone.getPhoneCode().length() != PHONENUM) {
            model.addAttribute("message", "手机号不正确");
            return "phone";
        }
        Admin isExist = adminService.judgeIsExist(phone.getPhoneCode());
        if (isExist == null) {
            model.addAttribute("message", "手机号不存在");
            return "phone";
        }
        String verification = phone.getVerificationCode();
        String code = (String) session.getAttribute("code");
        if ("".equals(verification)) {
            code = "";
            Random random = new Random();
            for (int i = 0; i < SIX; i++) {
                code += random.nextInt(TEN);
            }
            session.setAttribute("code", code);
            String url = "http://v.juhe.cn/sms/send";
            Map params = new HashMap();
            params.put("mobile", phone.getPhoneCode());
            params.put("tpl_id", "129706");
            params.put("tpl_value", "#code#=" + code);
            params.put("key", "3b8aa3b9a4cfeb1c4e1b223bcc1847bb");
            try {
                net(url, params, "GET");
            } catch (Exception e) {
                e.printStackTrace();
            }
            model.addAttribute("message", "验证码已发送");
        } else if (null == code) {
            model.addAttribute("message", "验证码不正确");
            return "phone";
        } else if ((code).equals(verification)) {
            if ("0".equals(isExist.getPower())) {
                session.setAttribute("phone", isExist);
                return "admin/superAdmin/superAdminIndex";
            } else {
                session.setAttribute("phone", isExist);
                return "admin/adminIndex";
            }
        } else {
            model.addAttribute("message", "验证码不正确");
            return "phone";
        }
        return "phone";
    }
    /**
     * @param strUrl 请求地址
     * @param params 请求参数
     * @param method 请求方法
     * @return  网络请求字符串
     * @throws Exception 抛错
     */
    public static String net(String strUrl, Map params, String method)
            throws Exception {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        String rs = null;
        try {
            StringBuffer sb = new StringBuffer();
            if (method == null || method.equals("GET")) {
                strUrl = strUrl + "?" + urlencode(params);
            }
            URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            if (method == null || method.equals("GET")) {
                conn.setRequestMethod("GET");
            } else {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
            }
            conn.setRequestProperty("User-agent", userAgent);
            conn.setUseCaches(false);
            conn.setConnectTimeout(DEF_CONN_TIMEOUT);
            conn.setReadTimeout(DEF_READ_TIMEOUT);
            conn.setInstanceFollowRedirects(false);
            conn.connect();
            if (params != null && method.equals("POST")) {
                try {
                    DataOutputStream out =
                            new DataOutputStream(conn.getOutputStream());
                    out.writeBytes(urlencode(params));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            InputStream is = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, DEF_CHATSET));
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                sb.append(strRead);
            }
            rs = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return rs;
    }
    /**
     * 将map型转为请求参数型
     * @param data Map
     * @return String
     */
    public static String urlencode(Map<String, String> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry i : data.entrySet()) {
            try {
                sb.append(i.getKey())
                        .append("=")
                        .append(URLEncoder.encode(i.getValue() + "", "UTF-8"))
                        .append("&");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
