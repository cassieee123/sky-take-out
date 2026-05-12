package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
//import jdk.vm.ci.meta.Local;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();//这里是我前端传入的密码123456

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对  这里需要进行完善
        //下面将前端传入的密码进行md5加密，和数据库中存储的密码进行比对
        //这里只是为了防止数据库中存放的密码   直接泄漏了  所以进行一下加密
        //数据库中存储的密码 已经使用 md5进行加密过了
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            //所有程序中抛出的异常信息都是，设定好的要抛出的数据，MessageConstant
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定  0 表示被锁定  1表示正常
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }


    //先创建一个employee实体类，然后把DTO的数据拷贝到实体类当中，对剩下的属性进行赋值
    //可以使用BeanUtils工具类中的copyProperties方法来对对象进行拷贝，前提是对象的属性有一部分是相同的。
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);//对象属性拷贝
        //下面设置其他的对象属性
        employee.setStatus(StatusConstant.ENABLE);//设置该对象是否是可用的
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));//将字符串转换成字节数组
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        employee.setCreateUser(BaseContext.getCurrentId());//TODO后续需要修改为当前登录用户的id
        //在登录的时候jwt令牌中会携带当前登录用户的id，所以可以解析出来id
        //jwt解析出来员工id之后  如何传给Service的save方法？
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.insert(employee);
    }

    @Override
    public PageResult PageQuery(EmployeePageQueryDTO employeePageQueryDTO) {//DTO将想查询的页码和记录数都有了
        //
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());//将页码和每页记录数传入
        //page是固定的，Employee是每个用户的信息
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        //要将page对象处理为PageResult对象
        long total = page.getTotal();
        List<Employee> result = page.getResult();
        return new PageResult(total, result);
        //PageHelper的startPage方法可以通过传入的参数自动设置Limit，传入的是页码和记录数，好处是：字符串的拼接不用自己做
        //底层实现：
        //Page是PageHelper插件定义的一个泛型类，是一个固定的返回类型
    }
}
