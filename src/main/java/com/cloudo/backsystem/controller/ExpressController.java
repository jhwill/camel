package com.cloudo.backsystem.controller;

import com.cloudo.backsystem.entity.ExpressMappingEntity;
import com.cloudo.backsystem.service.ExpressService;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@RequestMapping("express")
public class ExpressController {
    Logger logger = LoggerFactory.getLogger("ExpressController");

    @Autowired
    private ExpressService expressService;

    /**
     * 通过trackingId获取物流信息
     * @param trackingNumber 物流单号
     * @param trackingId 用于查询物流的唯一码
     */
    @RequestMapping("getEuExpressInfo")
    @ResponseBody
    public void getEuExpressInfo(String trackingNumber,String trackingId){
        expressService.getEuExpressInfo(trackingNumber,trackingId);
    }
    /**
     * 通过包裹单号查询物流信息
     * @param parcelNumber
     * @return
     */
    @RequestMapping("getExpressInfo")
    @ResponseBody
    public String getExpressInfo(String parcelNumber,Integer type){
        if (type == null){
            type = 1;
        }
        String result = expressService.getExpressInfo(parcelNumber,type);

        return result;
    }

    /**
     * 通过物流单号查询跟收货人手机后四位查询顺丰物流信息
     * @param expressSn
     */
    @RequestMapping("querySHUNFENG")
    @ResponseBody
    public Object querySHUNFENG(String expressSn,String checkPhoneNo) throws IOException, DocumentException {
        return expressService.querySHUNFENG(expressSn,checkPhoneNo);
    }

    /**
     * 通过包裹单号查询圆通物流信息
     * @param parcelSn
     */
    @RequestMapping("queryYUANTONG")
    @ResponseBody
    public Object queryYUANTONG(String parcelSn) throws IOException{
        return expressService.queryZHONGTONG(parcelSn);
    }

    @RequestMapping("queryEpassExpress")
    @ResponseBody
    public Object queryEpassExpress(String biz_content){
        return expressService.queryEpassExpress(biz_content);
    }

    /**
     * 商家物流信息回推
     * @param parcelNumber
     * @param dateTime
     * @param msg
     * @param location
     * @return
     */
    @RequestMapping(value = "saveMerchantsExpressInfo",method = RequestMethod.POST)
    @ResponseBody
    public Object saveMerchantsExpressInfo(String parcelNumber,String dateTime,String msg,String location){
        return expressService.saveMerchantsExpressInfo(parcelNumber,dateTime,msg,location);
    }

    /**
     * 初始化包裹单任务
     * @param parcelNumber
     * @param checkPhoneNo
     * @return
     */
    @RequestMapping(value = "saveParcelNumber",method = RequestMethod.POST)
    @ResponseBody
    public Object saveParcelNumber(String parcelNumber,String checkPhoneNo){
        return expressService.saveParcelNumber(parcelNumber,checkPhoneNo);
    }

    @RequestMapping(value = "queryExpress",method = RequestMethod.GET)
    @ResponseBody
    public Object queryExpress(String code,String expressNo,String checkPhoneNo){
        return expressService.queryExpress(code,expressNo,checkPhoneNo);
    }
}
