package com.ky.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.ky.gmall.annotations.LoginRequired;
import com.ky.gmall.beans.OmsOrder;
import com.ky.gmall.beans.PaymentInfo;
import com.ky.gmall.payment.config.AlipayConfig;
import com.ky.gmall.service.OrderService;
import com.ky.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Reference
    PaymentService paymentService;

    @Reference
    OrderService orderService;


    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String alipayCallBackReturn(HttpServletRequest request, ModelMap modelMap){

        //回调请求中获取支付宝的参数
        String sign = request.getParameter("sign");
        String outTradeNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");
        String tradeStatus = request.getParameter("trade_status");
        String totalAmount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String callbackContent = request.getQueryString();

        //通过支付宝的paramsMap进行签名验证,2.0版本的接口将paramsMap参数去掉,导致同步请求没法验签
        if (StringUtils.isNotBlank(sign)){
            //验签成功
            //更新用户的支付状态
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(outTradeNo);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(tradeNo); //支付宝的交易凭证号
            paymentInfo.setCallbackContent(callbackContent); //回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            //支付成功后,引起系统服务->订单系统的更新->库存服务->物流
            //调用mq发送支付成功的消息
            paymentService.updatePayment(paymentInfo);
        }

        return "finish";
    }

    @RequestMapping("mx/submit")
    @LoginRequired(loginSuccess = true)
    public String mx(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap map){

        return "finish";
    }

    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelmap){
        //获得一个支付宝请求的客户端(不是一个链接,是一个封装好的http表单请求)
        String form = null;
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        //回调函数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",0.01);
        map.put("subject","支付提交测试");
        map.put("timeout_express","2m");

        String param = JSON.toJSONString(map);

        alipayRequest.setBizContent(param);

        try {
            form = alipayClient.pageExecute(alipayRequest).getBody();//调用SDK生成表单

        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        //生成并保存用户的支付信息
        OmsOrder omsOrder = orderService.getOrderByOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject("测试商品一件");
        paymentInfo.setTotalAmount(totalAmount);
        paymentService.savePaymentInfo(paymentInfo);

        //向消息中间件发送一个检查支付状态(支付服务消费)的延迟消息队列
        paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);


        //提交请求到支付宝
        return form;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelmap){
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");


        modelmap.put("outTradeNo",outTradeNo);
        modelmap.put("totalAmount",totalAmount);
        return "index";
    }
}
