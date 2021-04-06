package work.cxlm.filecase.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import work.cxlm.filecase.dao.mapper.UserMapper;
import work.cxlm.filecase.domain.User;
import work.cxlm.filecase.service.UserService;
import work.cxlm.filecase.service.base.BaseService;
import work.cxlm.filecase.service.base.BaseServiceImpl;

/**
 * create 2021/4/1 21:36
 *
 * @author Chiru
 */
@Service
@Slf4j
public class UserServiceImpl extends BaseServiceImpl<User, Integer> implements UserService {

    @Autowired
    protected UserServiceImpl(UserMapper mapper) {
        super(mapper);
    }

}
