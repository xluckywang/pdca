package com.jxufe.halu.controller;

import com.alibaba.fastjson.JSONArray;
import com.jxufe.halu.annotation.UpdateDateAnnotation;
import com.jxufe.halu.model.Project;
import com.jxufe.halu.model.Task;
import com.jxufe.halu.model.User;
import com.jxufe.halu.service.ITaskService;
import com.jxufe.halu.service.TaskServiceImpl;
import com.jxufe.halu.util.DateUtil;
import com.jxufe.halu.util.Tree;
import org.apache.ibatis.jdbc.Null;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.swing.tree.ExpandVetoException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

@Controller
@RequestMapping("/task")
@SessionAttributes({"projectID"})
public class TaskController {

    private ITaskService service = new TaskServiceImpl();


    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public ModelAndView toIndex(
            @RequestParam("projectID") String projectId,
            ModelMap map
    ) {
        map.put("projectID", projectId);
        return new ModelAndView("task");
    }


    @RequestMapping("/tree")
    public @ResponseBody
    Object getTaskTreeBy(HttpServletRequest request) {
        Map<String, Object> rs = new HashMap<String, Object>();
        rs.put("data", null);
        rs.put("status", false);
        rs.put("message", "获取失败");
        String projectId = null;
        try {
            projectId = (String) request.getParameter("projectID");
            List<Tree<Task>> taskTree = service.getTaskTreeByProjectId(projectId);
            JSONArray taskJsonArray = new JSONArray();
            String[] methods = {"getTags"};
            String[] keys = {"tags"};
            for (Tree task : taskTree) {
                taskJsonArray.add(task.toJson(methods, keys));
            }
            rs.put("status", true);
            rs.put("data", taskJsonArray);
            rs.put("message", "查询成功");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return rs;
        }
    }

    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public @ResponseBody
    Map update(@RequestBody Map task) {
        Map<String, Object> rs = new HashMap<String, Object>();
        rs.put("status", false);
        rs.put("message", "更新失败");
        try {
            Task databaseTask = service.findTaskById((String) task.get("taskId"));
            if (!(databaseTask instanceof Task)) throw new Exception("更新异常");
            Task updateTask = new Task((String) task.get("taskId"),
                    (String) task.get("name"),
                    databaseTask.getCreateDate(),
                    DateUtil.getCurrentTimestamp(),
                    databaseTask.getTaskType(),
                    (String) task.get("description"),
                    databaseTask.getPTaskId(),
                    databaseTask.getProjectId(),
                    databaseTask.getTno()
            );
            updateTask.setNodeProgress(databaseTask.getNodeProgress());
            updateTask.setProgress(databaseTask.getProgress());
            int updateNum = service.updateTask(updateTask);
            if (updateNum != 1) throw new Exception("添加异常");
            rs.put("status", true);
            rs.put("message", "更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            rs.put("message", "更新异常");
        } finally {
            return rs;
        }
    }

    @RequestMapping(value = "/add/{type}", method = RequestMethod.POST)
    public @ResponseBody
    Map addTask(@RequestBody Map body,@PathVariable("type") String type,HttpServletRequest request) {
        String projectId  = "";
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("status", false);
        result.put("message", "添加失败");
        try {
            String pTaskId = (String) body.get("pTaskId");
            String childNameKey = "taskName";
            String childDescriptionKey = "description";
            Task typeTask = new Task();
            int increateRs = 0;
            switch (type.toUpperCase()) {
                case "T":
                    if (service.isHasChild(pTaskId)) {
                        break;
                    }
                    String nameKey = "TypeTaskName";
                    String descriptionKey = "TypeTaskDescription";
                    String[] typeList = {"P", "D", "C", "A"};
                    List<Task> taskList = new ArrayList<Task>();
                    ArrayList bodyTypeTasks = (ArrayList)body.get("typeTask");
                    for (int i = 0; i < 4; ++i) {
                        typeTask = new Task();
                        Map typeTaskMap = (Map) bodyTypeTasks.get(i);
//                        Task task = new Task();
//                        String taskOfType = typeList[i];
//                        String name = taskOfType + nameKey;
//                        String description = taskOfType + descriptionKey;
//                        String startDateKey = taskOfType + "TypeStartDate";
//                        long startDateLong = DateUtil.date2TimeStampLong((String) body.get(startDateKey), "yyyy-MM-dd HH:mm:ss");
//                        Timestamp startTime = new Timestamp(startDateLong);
//                        String endDateKey = taskOfType + "TypeEndDate";
//                        long endDateLong = DateUtil.date2TimeStampLong((String) body.get(endDateKey), "yyyy-MM-dd HH:mm:ss");
//                        Timestamp endTime = new Timestamp(endDateLong);
//                        task.setTaskName((String) body.get(name));
//                        task.setDescription((String) body.get(description));
//                        task.setTaskType(typeList[i]);
//                        task.setPTaskId(pTaskId);
//                        task.setStartDate(startTime);
//                        task.setEndDate(endTime);
//                        task.setCreateDate(new Timestamp(System.currentTimeMillis()));
//                        task.setProjectId(projectId);
//                        taskList.add(task);
                        typeTask.setTaskType(((String) typeTaskMap.get("type")).toUpperCase());
                        typeTask.setCreateDate(DateUtil.getCurrentTimestamp());
                        typeTask.setProjectId((String) typeTaskMap.get("projectId"));
                        typeTask.setProgress("0");
                        typeTask.setDescription((String) typeTaskMap.get("description"));
                        typeTask.setStartDate(new Timestamp(DateUtil.date2TimeStampLong((String)typeTaskMap.get("startDate"),"yyyy-MM-dd HH:mm:ss")));
                        typeTask.setEndDate(new Timestamp(DateUtil.date2TimeStampLong((String)typeTaskMap.get("endDate"),"yyyy-MM-dd HH:mm:ss")));
                        typeTask.setTaskName((String) typeTaskMap.get("name"));
                        typeTask.setNodeProgress(typeTaskMap.get("progress")+"");
                        typeTask.setPTaskId((String) typeTaskMap.get("pTaskId"));
                        taskList.add(typeTask);
                    }
                    increateRs = service.increateTypeTask(taskList);
                    if (increateRs != 4) {
                        throw new Exception("添加异常");
                    }
                    break;
                case "P":
                case "D":
                case "C":
                case "A":
                    long startDateLong = DateUtil.date2TimeStampLong((String) body.get("startDate"), "yyyy-MM-dd HH:mm:ss");
                    Timestamp startTime = new Timestamp(startDateLong);
                    long endDateLong = DateUtil.date2TimeStampLong((String) body.get("endDate"), "yyyy-MM-dd HH:mm:ss");
                    projectId = (String)body.get("projectId");
                    Timestamp endTime = new Timestamp(endDateLong);
                    typeTask.setTaskType("T");
                    typeTask.setDescription((body.get(childDescriptionKey) == null) ? "" : (String) body.get(childNameKey));
                    typeTask.setTaskName((String) body.get(childNameKey));
                    typeTask.setProjectId(projectId);
                    typeTask.setPTaskId((String)body.get("pTaskId"));
                    typeTask.setTaskName((String) body.get("name"));
                    typeTask.setProgress("0");
                    typeTask.setNodeProgress((String) body.get("progress"));
                    typeTask.setDescription((String) body.get("description"));
                    typeTask.setStartDate(startTime);
                    typeTask.setEndDate(endTime);
                    typeTask.setCreateDate(new Timestamp(System.currentTimeMillis()));
                    typeTask.setTno(service.numChild(pTaskId) + 1);
                    service.addTask(typeTask);
                    break;
                case"R":
                    Task rootTask = new Task();
                    projectId = (String) body.get("projectId");
                    rootTask.setTaskType("T");
                    rootTask.setCreateDate(DateUtil.getCurrentTimestamp());
                    rootTask.setProjectId(projectId);
                    rootTask.setProgress("0");
                    rootTask.setDescription((String) body.get("description"));
                    int rootTaskCount = service.countRootTaskByProjectId(projectId);
                    rootTask.setTno(++rootTaskCount);
                    rootTask.setTno(rootTaskCount);
                    rootTask.setStartDate(new Timestamp(DateUtil.date2TimeStampLong((String)body.get("startDate"),"yyyy-MM-dd HH:mm:ss")));
                    rootTask.setEndDate(new Timestamp(DateUtil.date2TimeStampLong((String)body.get("endDate"),"yyyy-MM-dd HH:mm:ss")));
                    rootTask.setTaskName((String) body.get("name"));
                    rootTask.setNodeProgress((String) body.get("progress"));
                    service.addTask(rootTask);
                    break;
                default:
                    throw new Exception("任务类型异常");
            }
            result.put("status", true);
            result.put("message", "添加成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("message", "添加异常");
        } finally {
            return result;
        }
    }

//    @RequestMapping("/canAdd")
//    @ResponseBody
//    Map canAdd(HttpServletRequest request) {
//        Map result = new HashMap();
//        result.put("canAdd", false);
//        result.put("status", false);
//        result.put("message", "验证失败");
//        try {
//            String taskId = (String) request.getAttribute("taskId");
//            boolean canAdd = service.canAdd(taskId);
//            if(canAdd){
//                result.put("canAdd",true);
//                result.put("message","能添加");
//            }else {
//                result.put("message","不能添加");
//            }
//            result.put("status",true);
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.put("message","验证异常");
//        }
//        return result;
//    }

    @RequestMapping(value = "/overTask/{taskId}", method = RequestMethod.PUT)
    public @ResponseBody
    Map overTask(@PathVariable("taskId") String taskId) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("status", false);
        result.put("message", "结束失败");
        try {
            if (!service.isValidOver(taskId)) {
                throw new Exception("不能结束");
            }
            service.overTask(taskId);
            result.put("status", true);
            result.put("message", "结束任务");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("message", "结束异常");
        }
        return result;
    }

    @RequestMapping(value = "deleteNodeById",method = RequestMethod.DELETE)
    public  @ResponseBody
    Map deleteNodeById(HttpServletRequest req){
        Map result = new HashMap();
        result.put("status",false);
        result.put("message","删除失败");
        try{
            String taskId = (String) req.getParameter("taskId");
            int deleteNum = service.deleteNodeByTaskId(taskId);
            result.put("data",deleteNum);
            result.put("status",true);
            result.put("message","删除成功");
        }catch (Exception e){
            e.printStackTrace();
            result.put("message","删除异常");
        }
         return  result;
    }

    @RequestMapping("/chart/countAddTaskWeek")
    public @ResponseBody
    Map<String, Object> countAddWeek() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", false);
        result.put("message", "查询失败");
        try {
            Session session = SecurityUtils.getSubject().getSession();
            User user = (User) session.getAttribute("user");
            List<Map> data = service.countTaskByUserID(user.getUserID(), "week");
            result.put("data", data);
            result.put("status", true);
            result.put("message", "查询成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("message", "查询异常");
        }
        return result;
    }

    @RequestMapping("/chart/countType")
    public @ResponseBody
    Map<String, Object> countType() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", false);
        result.put("message", "查询失败");
        try {
            Session session = SecurityUtils.getSubject().getSession();
            User user = (User) session.getAttribute("user");
            List<Map> data = service.countType(user.getUserID());
            result.put("status", true);
            result.put("message", "查询成功");
            result.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("message", "查询异常");
        }
        return result;
    }

    /**
     * 扇形图
     *
     * @return
     */
    @RequestMapping("/chart/taskflow")
    public @ResponseBody
    Map<String, Object> taskflow() {
        Map result = new HashMap();
        result.put("status", false);
        result.put("message", "查询失败");
        try {
            User user = (User) SecurityUtils.getSubject().getSession().getAttribute("user");
            Map data = service.countProgressByUserId(user.getUserID());
            result.put("data", data);
            result.put("status", true);
            result.put("message", "查询成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("message", "查询异常");
        }
        return result;
    }

    /**
     * vue模板流量统计报表接口
     *
     * @return
     */
    @RequestMapping("/chart/serviceRequest")
    public @ResponseBody
    Map<String, Object> serviceRequest() {
        Map result = new HashMap();
        result.put("status", false);
        result.put("message", "查询失败");
        try {
            User user = (User) SecurityUtils.getSubject().getSession().getAttribute("user");
            Map data = service.updateWeekday(user.getUserID());
            result.put("status", true);
            result.put("message", "查询成功");
            result.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("message", "查询异常");
        }
        return result;
    }


}
