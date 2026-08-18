package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.db.TagDao;
import com.haleydu.cimoc.model.Tag;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TagManager {

    public static final long TAG_CONTINUE = -101;
    public static final long TAG_FINISH = -100;

    private TagDao mTagDao;

    @Inject
    public TagManager(TagDao tagDao) {
        mTagDao = tagDao;
    }

    public List<Tag> list() {
        return mTagDao.list();
    }

    public Tag load(String title) {
        return mTagDao.load(title);
    }

    public void insert(Tag tag) {
        long id = mTagDao.insert(tag);
        tag.setId(id);
    }

    public void update(Tag tag) {
        mTagDao.update(tag);
    }

    public void delete(Tag entity) {
        mTagDao.delete(entity);
    }

}
