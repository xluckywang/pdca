# pdca
项目进度管理系统(戴明环PDCA进行管控)

PDCA四个英文字母及其在PDCA循环中所代表的含义如下：   
1.  P（Plan）--计划，确定方针和目标，确定活动计划； 
2.  D（Do）--执行，实地去做，实现计划中的内容； 
3.  C（Check）--检查，总结执行计划的结果，注意效果，找出问题； 
4.  A（Action）--行动，对总结检查的结果进行处理，成功的经验加以肯定并适当推广、标准化；失败的教训加以总结，以免重现，未解决的问题放到下一个PDCA循环。

## 需求点
    1. 任务增加别名,无别名取前两个加省略号
    2. 类型任务时间总和等于任务时间
    3. 任务起始时间必须,终止时间非必须
    4. a类型任务终止时间不需要等于任务终止时间
    
## 2018/3/17
    1. 引入权限框架shiro(已完成)
    2. 添加权限模块
    3. 实现用户注册及修改密码