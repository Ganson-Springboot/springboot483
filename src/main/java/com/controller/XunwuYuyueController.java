
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 寻物认领
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/xunwuYuyue")
public class XunwuYuyueController {
    private static final Logger logger = LoggerFactory.getLogger(XunwuYuyueController.class);

    private static final String TABLE_NAME = "xunwuYuyue";

    @Autowired
    private XunwuYuyueService xunwuYuyueService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private GonggaoService gonggaoService;//公告信息
    @Autowired
    private ShiwuService shiwuService;//失物招领
    @Autowired
    private ShiwuYuyueService shiwuYuyueService;//失物认领
    @Autowired
    private XunwuService xunwuService;//寻物启示
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = xunwuYuyueService.queryPage(params);

        //字典表数据转换
        List<XunwuYuyueView> list =(List<XunwuYuyueView>)page.getList();
        for(XunwuYuyueView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        XunwuYuyueEntity xunwuYuyue = xunwuYuyueService.selectById(id);
        if(xunwuYuyue !=null){
            //entity转view
            XunwuYuyueView view = new XunwuYuyueView();
            BeanUtils.copyProperties( xunwuYuyue , view );//把实体数据重构到view中
            //级联表 寻物启示
            //级联表
            XunwuEntity xunwu = xunwuService.selectById(xunwuYuyue.getXunwuId());
            if(xunwu != null){
            BeanUtils.copyProperties( xunwu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setXunwuId(xunwu.getId());
            }
            //级联表 用户
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(xunwuYuyue.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody XunwuYuyueEntity xunwuYuyue, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,xunwuYuyue:{}",this.getClass().getName(),xunwuYuyue.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            xunwuYuyue.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<XunwuYuyueEntity> queryWrapper = new EntityWrapper<XunwuYuyueEntity>()
            .eq("xunwu_id", xunwuYuyue.getXunwuId())
            .eq("yonghu_id", xunwuYuyue.getYonghuId())
            .in("xunwu_yuyue_yesno_types", new Integer[]{1,2})
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        XunwuYuyueEntity xunwuYuyueEntity = xunwuYuyueService.selectOne(queryWrapper);
        if(xunwuYuyueEntity==null){
            xunwuYuyue.setInsertTime(new Date());
            xunwuYuyue.setXunwuYuyueYesnoTypes(1);
            xunwuYuyue.setCreateTime(new Date());
            xunwuYuyueService.insert(xunwuYuyue);
            return R.ok();
        }else {
            if(xunwuYuyueEntity.getXunwuYuyueYesnoTypes()==1)
                return R.error(511,"有相同的待审核的数据");
            else if(xunwuYuyueEntity.getXunwuYuyueYesnoTypes()==2)
                return R.error(511,"有相同的审核通过的数据");
            else
                return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody XunwuYuyueEntity xunwuYuyue, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,xunwuYuyue:{}",this.getClass().getName(),xunwuYuyue.toString());
        XunwuYuyueEntity oldXunwuYuyueEntity = xunwuYuyueService.selectById(xunwuYuyue.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            xunwuYuyue.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        if("".equals(xunwuYuyue.getXunwuYuyueText()) || "null".equals(xunwuYuyue.getXunwuYuyueText())){
                xunwuYuyue.setXunwuYuyueText(null);
        }
        if("".equals(xunwuYuyue.getXunwuYuyuePhoto()) || "null".equals(xunwuYuyue.getXunwuYuyuePhoto())){
                xunwuYuyue.setXunwuYuyuePhoto(null);
        }
        if("".equals(xunwuYuyue.getXunwuYuyueYesnoText()) || "null".equals(xunwuYuyue.getXunwuYuyueYesnoText())){
                xunwuYuyue.setXunwuYuyueYesnoText(null);
        }

            xunwuYuyueService.updateById(xunwuYuyue);//根据id更新
            return R.ok();
    }


    /**
    * 审核
    */
    @RequestMapping("/shenhe")
    public R shenhe(@RequestBody XunwuYuyueEntity xunwuYuyueEntity, HttpServletRequest request){
        logger.debug("shenhe方法:,,Controller:{},,xunwuYuyueEntity:{}",this.getClass().getName(),xunwuYuyueEntity.toString());

        XunwuYuyueEntity oldXunwuYuyue = xunwuYuyueService.selectById(xunwuYuyueEntity.getId());//查询原先数据

        if(xunwuYuyueEntity.getXunwuYuyueYesnoTypes() == 2){//通过
            XunwuEntity xunwuEntity = xunwuService.selectById(oldXunwuYuyue.getXunwuId());
            xunwuEntity.setShangxiaTypes(2);
            xunwuService.updateById(xunwuEntity);
//            xunwuYuyueEntity.setXunwuYuyueTypes();
        }else if(xunwuYuyueEntity.getXunwuYuyueYesnoTypes() == 3){//拒绝
//            xunwuYuyueEntity.setXunwuYuyueTypes();
        }
        xunwuYuyueEntity.setXunwuYuyueShenheTime(new Date());//审核时间
        xunwuYuyueService.updateById(xunwuYuyueEntity);//审核

        return R.ok();
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<XunwuYuyueEntity> oldXunwuYuyueList =xunwuYuyueService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        xunwuYuyueService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //.eq("time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
        try {
            List<XunwuYuyueEntity> xunwuYuyueList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            XunwuYuyueEntity xunwuYuyueEntity = new XunwuYuyueEntity();
//                            xunwuYuyueEntity.setXunwuYuyueUuidNumber(data.get(0));                    //报名编号 要改的
//                            xunwuYuyueEntity.setXunwuId(Integer.valueOf(data.get(0)));   //寻物启示 要改的
//                            xunwuYuyueEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            xunwuYuyueEntity.setXunwuYuyueText(data.get(0));                    //报名理由 要改的
//                            xunwuYuyueEntity.setXunwuYuyuePhoto("");//详情和图片
//                            xunwuYuyueEntity.setInsertTime(date);//时间
//                            xunwuYuyueEntity.setXunwuYuyueYesnoTypes(Integer.valueOf(data.get(0)));   //报名状态 要改的
//                            xunwuYuyueEntity.setXunwuYuyueYesnoText(data.get(0));                    //审核回复 要改的
//                            xunwuYuyueEntity.setXunwuYuyueShenheTime(sdf.parse(data.get(0)));          //审核时间 要改的
//                            xunwuYuyueEntity.setCreateTime(date);//时间
                            xunwuYuyueList.add(xunwuYuyueEntity);


                            //把要查询是否重复的字段放入map中
                                //报名编号
                                if(seachFields.containsKey("xunwuYuyueUuidNumber")){
                                    List<String> xunwuYuyueUuidNumber = seachFields.get("xunwuYuyueUuidNumber");
                                    xunwuYuyueUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> xunwuYuyueUuidNumber = new ArrayList<>();
                                    xunwuYuyueUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("xunwuYuyueUuidNumber",xunwuYuyueUuidNumber);
                                }
                        }

                        //查询是否重复
                         //报名编号
                        List<XunwuYuyueEntity> xunwuYuyueEntities_xunwuYuyueUuidNumber = xunwuYuyueService.selectList(new EntityWrapper<XunwuYuyueEntity>().in("xunwu_yuyue_uuid_number", seachFields.get("xunwuYuyueUuidNumber")));
                        if(xunwuYuyueEntities_xunwuYuyueUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(XunwuYuyueEntity s:xunwuYuyueEntities_xunwuYuyueUuidNumber){
                                repeatFields.add(s.getXunwuYuyueUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [报名编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        xunwuYuyueService.insertBatch(xunwuYuyueList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = xunwuYuyueService.queryPage(params);

        //字典表数据转换
        List<XunwuYuyueView> list =(List<XunwuYuyueView>)page.getList();
        for(XunwuYuyueView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Integer id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        XunwuYuyueEntity xunwuYuyue = xunwuYuyueService.selectById(id);
            if(xunwuYuyue !=null){


                //entity转view
                XunwuYuyueView view = new XunwuYuyueView();
                BeanUtils.copyProperties( xunwuYuyue , view );//把实体数据重构到view中

                //级联表
                    XunwuEntity xunwu = xunwuService.selectById(xunwuYuyue.getXunwuId());
                if(xunwu != null){
                    BeanUtils.copyProperties( xunwu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setXunwuId(xunwu.getId());
                }
                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(xunwuYuyue.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody XunwuYuyueEntity xunwuYuyue, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,xunwuYuyue:{}",this.getClass().getName(),xunwuYuyue.toString());
        Wrapper<XunwuYuyueEntity> queryWrapper = new EntityWrapper<XunwuYuyueEntity>()
            .eq("xunwu_yuyue_uuid_number", xunwuYuyue.getXunwuYuyueUuidNumber())
            .eq("xunwu_id", xunwuYuyue.getXunwuId())
            .eq("yonghu_id", xunwuYuyue.getYonghuId())
            .eq("xunwu_yuyue_text", xunwuYuyue.getXunwuYuyueText())
            .in("xunwu_yuyue_yesno_types", new Integer[]{1,2})
            .eq("xunwu_yuyue_yesno_text", xunwuYuyue.getXunwuYuyueYesnoText())
//            .notIn("xunwu_yuyue_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        XunwuYuyueEntity xunwuYuyueEntity = xunwuYuyueService.selectOne(queryWrapper);
        if(xunwuYuyueEntity==null){
            xunwuYuyue.setInsertTime(new Date());
            xunwuYuyue.setXunwuYuyueYesnoTypes(1);
            xunwuYuyue.setCreateTime(new Date());
        xunwuYuyueService.insert(xunwuYuyue);

            return R.ok();
        }else {
            if(xunwuYuyueEntity.getXunwuYuyueYesnoTypes()==1)
                return R.error(511,"有相同的待审核的数据");
            else if(xunwuYuyueEntity.getXunwuYuyueYesnoTypes()==2)
                return R.error(511,"有相同的审核通过的数据");
            else
                return R.error(511,"表中有相同数据");
        }
    }

}

