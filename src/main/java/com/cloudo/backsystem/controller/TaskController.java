package com.cloudo.backsystem.controller;

import com.cloudo.backsystem.service.TaskService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @RequestMapping("getUncheckParcelSn")
    @ResponseBody
    public Object getUncheckParcelSn(String startTime,String endTime){
        if(StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)){
            //默认查一天的快递信息
            endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            startTime = LocalDateTime.now().minusHours(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return taskService.getUncheckParcelSn(startTime,endTime);
    }

    /**
     * 创建包裹单物流信息任务
     * @param
     */
    @RequestMapping("createExpressTask")
    @ResponseBody
    public Object createExpressTask(){
        try {
            return taskService.createExpressTask();
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 创建下载物流信息任务
     * @param
     */
    @RequestMapping("downloadExpressInfo")
    @ResponseBody
    public Object downloadExpressInfo(){
        try {
            return taskService.downloadExpressInfo();
        }catch (Exception e){
            return null;
        }
    }
}
