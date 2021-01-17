public enum Operation{
    push,
    popn,
    loca,
    arga,
    globa,
    load,
    store_64,
    stackalloc,
    add_i,
    add_f,
    sub_i,
    sub_f,
    mul_i,
    mul_f,
    div_i,
    div_f,
    not,
    //int大小比较
    cmp_i,
    //浮点数大小比较
    cmp_f,
    neg_i,
    neg_f,
    set_lt,
    set_gt,
    br_t,
    br_f,
    br,
    call,
    ret,
    callname,
    itof,
    ftoi,
    ;


    Operation() {

    }


}
