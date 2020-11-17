package com.cloudo.backsystem.service;

import com.cloudo.backsystem.dao.ExpressMappingDao;
import com.cloudo.backsystem.dao.ExpressValueDao;
import com.cloudo.backsystem.dao.TaskDao;
import com.cloudo.backsystem.entity.ExpressMappingEntity;
import com.cloudo.backsystem.entity.ExpressValueEntity;
import com.cloudo.backsystem.entity.TaskEntity;
import com.cloudo.backsystem.util.HttpClientUtils;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;;
import java.util.stream.Collectors;

@Service
public class TaskService {
    Logger logger = LoggerFactory.getLogger("TaskService");

    private static String GET_PARCEL_SN_URL = " ";
    private static String GET_EXPRESS_SN_URL = " ";

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private ExpressMappingDao expressMappingDao;

    @Autowired
    private ExpressValueDao expressValueDao;

    @Autowired
    private ExpressService expressService;

    public Object getUncheckParcelSn(String startTime,String endTime) {
        Map<String,String> queryMap = new HashMap<>();
        queryMap.put("startTime",startTime);
        queryMap.put("endTime",endTime);
        String parcelSns = HttpClientUtils.createHttpGet(GET_PARCEL_SN_URL,new HashMap<>(),queryMap);
        if (StringUtils.isBlank(parcelSns)){
            return "未获取需要处理包裹单";
        }
        Gson gson = new Gson();
        List<String> parcelSnList = gson.fromJson(parcelSns,List.class);
        logger.error("获取包裹单:    "+parcelSns);
        List<TaskEntity> unCheckedParcels = new ArrayList<>();
        parcelSnList.stream().distinct().forEach(e->{
            TaskEntity taskEntity = taskDao.findByParcelSn(e);
            if(taskEntity == null){
                taskEntity = new TaskEntity();
                taskEntity.setParcelSn(e);
                taskEntity.setStatus("0");
                unCheckedParcels.add(taskEntity);
            }
        });
        taskDao.saveAll(unCheckedParcels);
        return unCheckedParcels.size();
    }

    /**
     * 添加为处理包裹单到物流任物表
     * @return
     */
    public Object createExpressTask() {
        String status = "0";
        List<TaskEntity> taskEntities = taskDao.findByStatus(status);
        if(taskEntities == null || taskEntities.size() == 0){
            return "未查询到需要处理的包裹单";
        }
        logger.error("开始处理包裹单 "+taskEntities.size());
        taskEntities.forEach(e->{
            String parcelSn = e.getParcelSn();
            Map<String,String> param = new HashMap<>();
            param.put("parcelNumber",parcelSn);
            //获取包裹单对应物流单号及收货人手机号后四位  格式 eu_expressSn,epass_expressSn,cn_expressSn,checkPhoneNo
            String result = HttpClientUtils.createHttpGet(GET_EXPRESS_SN_URL,new HashMap<>(),param);
            logger.error("获取快递单号 "+ result);
            if(StringUtils.isBlank(result)){
                return;
            }
            List<String> expressSnList = Arrays.asList(result.split(","));
            for (int i = 0; i < 3; i++) {
                String expressSn = expressSnList.get(i);
                int sign = i+1;
                if (!"".equals(expressSn)){
                    ExpressMappingEntity expressMappingEntity = expressMappingDao.findByParcelSnAndSign(parcelSn,sign);
                    if(expressMappingEntity == null){
                        expressMappingEntity = new ExpressMappingEntity();
                        expressMappingEntity.setParcelSn(parcelSn);
                        expressMappingEntity.setSign(sign);
                        expressMappingEntity.setStatus("0");
                    }
                    expressMappingEntity.setExpressSn(expressSn);
                    expressMappingDao.save(expressMappingEntity);
                }
            }
            String checkPhoneNo = expressSnList.get(3);
            TaskEntity taskEntity = taskDao.findByParcelSn(parcelSn);
            taskEntity.setCheckPhoneNo(checkPhoneNo);
            taskEntity.setStatus("1");
            taskDao.save(taskEntity);
        });
        return "成功";
    }

    /**
     * 下载未完成物流信息
     * @return
     */
    public Object downloadExpressInfo() {
        String status = "0";
        List<ExpressMappingEntity> expressMappingEntities = expressMappingDao.findByStatus(status);
        if (expressMappingEntities == null || expressMappingEntities.size() == 0){
            return "为查询到需要下载的物流信息";
        }

        Map<Integer,List<ExpressMappingEntity>> mapList = expressMappingEntities.stream().collect(Collectors.groupingBy(ExpressMappingEntity::getSign));
        List<ExpressMappingEntity> euExpressList = mapList.get(1);
        List<ExpressMappingEntity> cnExpressList = mapList.get(3);

        if (euExpressList !=null && euExpressList.size() != 0) {
            euExpressList.forEach(e -> expressService.getEuExpressInfo(e.getExpressSn(), e.getParcelSn()));
        }

        if(cnExpressList != null && cnExpressList.size() != 0 ){
            cnExpressList.forEach(e->{
                String expressSn = e.getExpressSn();
                String parcelSn = e.getParcelSn();
                String info;
                try {
                    TaskEntity taskEntity = taskDao.findByParcelSn(parcelSn);
                    if (taskEntity == null || taskEntity.getCheckPhoneNo() == null){
                        return;
                    }
                    info = expressService.querySHUNFENG(expressSn, taskEntity.getCheckPhoneNo());
                    if ("[]".equals(info)){

                        info = expressService.queryYUANTONG(expressSn);
                    }
                }catch (Exception exception){
                    return;
                }
                ExpressValueEntity expressValueEntity = expressValueDao.findByExpressSn(expressSn);
                if(expressValueEntity == null){
                    expressValueEntity = new ExpressValueEntity();
                    expressValueEntity.setExpressSn(expressSn);
                }
                expressValueEntity.setInfo(info);
                expressValueDao.save(expressValueEntity);
            });
        }
        return "成功";
    }
}
