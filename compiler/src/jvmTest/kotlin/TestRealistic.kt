import kotlin.test.Test
import kotlin.test.assertEquals

class TestRealistic {

    @Test
    fun sin() {
        val result = interpret("""
            fn sin(_x) {
              _x %= 360e3
              _sign = if (_x > 180e3 || _x < -180e3) -1 else 1
              _x %= 180e3

              if (_x < 0) {
                _sign *= -1
                _x += 180e3
              }

              _180 = 180e3 - _x
              _180s = _180 * _180 / 1e7
              _180 *= _x

              // https://scholarworks.umt.edu/cgi/viewcontent.cgi?article=1313&context=tme
              _result = (2e6 * _180) / (40500e6 - _180)
                + (31e2 * _180) / 648000e2
                + (_180s * _x * _x / 58320000)

              return (_result + 0.5e3) / 1e3 * _sign
            }
            
            fn main() {
                sin0 = sin(0)
                pause(4)
                sin45 = sin(45000)
                pause(4)
                sin90 = sin(90000)
                message("%stat.player/sin0% %stat.player/sin45% %stat.player/sin90%")
            }
        """.trimIndent())
        assertEquals("0 707 1000", result, "Wrong result")
    }

    @Test
    fun bit_packing() {
        val result = interpret("""
            #inline
            fn read_bits(_stat, _offset, _bits) {
              _temp = _stat
              _temp /= 2^_offset
              _temp %= 2^_bits
              return _temp
            }

            #inline
            fn set_bits(_stat, _offset, _value, _prev) {
              _stat -= _prev * 2^_offset
              _stat += _value * 2^_offset
            }

            fn main() {
              data = 0b0101_0001_1101_0111
              //          5    1   13    7

              #for (i in 0..3) {
                // read bit at ${"\$i"} * 4
                value = read_bits(data, ${"\$i"}*4, 4)
                message("&0%stat.player/value%")
                // increment value by 1
                set_bits(data, ${"\$i"}*4, value + 1, value)
              }

              message("")

              // re-read bits
              #for (i in 0..3) {
                value = read_bits(data, ${"\$i"}*4, 4)
                message("%stat.player/value%")
              }
            }
        """.trimIndent())
        assertEquals("7\n13\n1\n5\n\n8\n14\n2\n6", result, "Wrong result")
    }

    @Test
    fun dungeons() {
        compile("""
            fn dgn_move(_door_id, _sampler_id) {
                _prev_index = dcx_index

                if (_door_id == 0 && dcx_position == 0) {
                    // moving backwards at position 0

                    dcx_read_ctx(dcx_parent_index) // read context from parent index
                    _door_id = (_prev_index - dcx_child_index) + 1
                    _room_id = dcx_read_room(dcx_position, _sampler_id)
                    dgn_tp(_room_id, _door_id)
                    return 0
                }

                if (_door_id == 0) {
                    // moving backwards
                    dcx_position -= 1

                    _room_id = dcx_read_room(dcx_position, _sampler_id)
                    dgn_tp(_room_id, 1)
                    return 0
                }

                if (_door_id == 1 && dcx_position < 5) {
                    // moving forwards
                    dcx_position += 1

                    _room_id = dcx_read_room(dcx_position, _sampler_id)
                    dgn_tp(_room_id, 0)
                    return 0
                }

                if (dcx_child_index == 0 && dcx_stack_size == 31) {
                    err_max_ctx_stack()
                    return 0
                }

                // moving forwards into new context

                if (dcx_child_index == 0) {
                    // we never ended up splitting, so just set params here

                    // set child offset to new ctx index
                    dcx_child_index = dcx_stack_size
                    dcx_stack_size += 1
                }

                _index = (dcx_child_index + _door_id) - 1
                dcx_read_ctx(_index)

                dcx_parent_index = _prev_index // update parent context

                _room_id = dcx_read_room(dcx_position, _sampler_id)
                dgn_tp(_room_id, 0)
            }


            fn dgn_progress() {
                if (dcx_show_prog == false) {
                    return 0
                }
                if (dcx_stack_min > dcx_stack_size) {
                    _total = dcx_stack_min
                } else {
                    _total = dcx_stack_size
                }
                _current = dcx_stack_size - dcx_alive
                _percent = _current * 100 / _total
                action_bar("&bDungeon Progress: &f%stat.player/_percent%%")
            }

            #inline
            fn connect(room, id, location) {
                if (_room_id == room && _door_id == id) {
                    tp(location)
                    return 0
                }
            }

            fn dgn_tp(_room_id, _door_id) {
                connect(1, 0, <-4, 3, 60.5, 180, 0>)
                connect(1, 1, <-6.5, 3, 55, -90, 0>)
                connect(1, 2, <-1.5, 3, 55, 90, 0>)

                connect(2, 0, <-6, 3, 49.5, 180, 0>)
                connect(2, 1, <-6, 3, 43.5, 0, 0>)

                connect(3, 0, <-2, 3, 49.5, 180, 0>)

                connect(4, 0, <4, 3, 49.5, 180, 0>)
                connect(4, 1, <6.5, 8, 42, 90, 0>)

                connect(5, 0, <-13, 3, 55.5, 0, 0>)
            }

            fn dcx_reset() {
                #for (i in 0..31) {
                    dcx_ctx_${"$"}i = 0
                }
                dcx_stack_size = 1
                dcx_alive = 1
                dcx_index = 0
                #for (i in 0..5) {
                    dcx_room_${"$"}i = 0
                }
                dcx_parent_index = 0
                dcx_child_index = 0
                dcx_position = 0
            }

            fn dcx_read_ctx(_index) {
                message("&cReading CTX: %stat.player/_index%")
                dcx_write_ctx()

                // save ids
                dcx_index = _index

                #for (i in 0..31) {
                    if (_index == ${"$"}i) { _dcx_buffer = dcx_ctx_${"$"}i }
                }

                // read metadata
                dcx_parent_index = read_bits(_dcx_buffer, 0, 5) // 0-31
                dcx_child_index = read_bits(_dcx_buffer, 5, 5)
                dcx_position = read_bits(_dcx_buffer, 10, 3)

                // read room ids
                dcx_room_0 = read_bits(_dcx_buffer, 14, 8)
                dcx_room_1 = read_bits(_dcx_buffer, 22, 8)
                dcx_room_2 = read_bits(_dcx_buffer, 30, 8)
                dcx_room_3 = read_bits(_dcx_buffer, 38, 8)
                dcx_room_4 = read_bits(_dcx_buffer, 46, 8)
                dcx_room_5 = read_bits(_dcx_buffer, 54, 8)
            }

            fn dcx_write_ctx() {
                _dcx_buffer = 0 // ensure buffer is clear

                // write metadata
                write_bits(_dcx_buffer, 0, dcx_parent_index, 0)
                write_bits(_dcx_buffer, 5, dcx_child_index, 0)
                write_bits(_dcx_buffer, 10, dcx_position, 0)

                // write room ids
                write_bits(_dcx_buffer, 14, dcx_room_0, 0)
                write_bits(_dcx_buffer, 22, dcx_room_1, 0)
                write_bits(_dcx_buffer, 30, dcx_room_2, 0)
                write_bits(_dcx_buffer, 38, dcx_room_3, 0)
                write_bits(_dcx_buffer, 46, dcx_room_4, 0)
                write_bits(_dcx_buffer, 54, dcx_room_5, 0)

                #for (i in 0..31) {
                    if (dcx_index == ${"$"}i) { dcx_ctx_${"$"}i = _dcx_buffer }
                }
            }

            /*
            RULES:

            - cannot generate a dead end in the same context as a split (this would waste one context)
            - cannot generate a dead end if this is the only alive context and we have not reached the minimum contexts
            - must generate a dead end if we are at position five and have reached the maximum contexts
            - cannot generate a split if we have reached the maximum contexts
            - cannot generate a split in the same context as another split
            */
            fn sampler0() {
                message("Generating room...")
                _split = true
                _end = true
                if (dcx_child_index > 0) {
                    _end = false
                    _split = false
                }
                if (dcx_alive == 1 && dcx_stack_min > dcx_stack_size) {
                    _end = false
                }
                if (dcx_stack_max == dcx_stack_size) {
                    _split = false
                }

                _rand = "%random.int/1 5%"

                if (_end == false) {
                    _rand = "%random.int/1 4%"
                }
                if (_split == false) {
                    _rand = "%random.int/2 5%"
                }
                if (_split == false && _end == false) {
                    _rand = "%random.int/2 4%"
                }

                if (dcx_stack_max == dcx_stack_size && dcx_position == 5) { // must generate a dead end
                    _rand = 4
                }

                if (_rand <= 1) { // split
                    dcx_alive += 1
                    dcx_child_index = dcx_stack_size
                    dcx_stack_size += 2
                    return 1
                }
                if (_rand <= 2) {
                    return 2
                }
                if (_rand <= 3) {
                    return 4
                }
                if (_rand <= 4) { // dead end
                    dcx_alive -= 1
                    return 3
                }
            }


            fn dcx_read_room(_index, _sampler_id) {
                #for (i in 0..5) {
                    if (_index == ${"$"}i) { _room_id = dcx_room_${"$"}i }
                }

                if (_room_id == 0) {
                    // we need to generate this room
                    _room_id = sampler0()
                    dcx_write_room(dcx_position, _room_id)
                }
                return _room_id
            }

            fn dcx_write_room(_index, _room_id) {
                #for (i in 0..5) {
                    if (_index == ${"$"}i) { dcx_room_${"$"}i = _room_id }
                }
            }

            #inline
            fn read_bool(_stat, _offset) {
                return read_bits(_stat, _offset, 1)
            }

            #inline
            fn write_bool(_stat, _offset, _value, _prev) {
                write_bits(_stat, _offset, _value, _prev)
            }

            #inline
            fn write_bool_unsafe(_stat, _offset, _value) {
                if (_value == true) {
                    _stat += 2^_offset
                } else {
                    _stat -= 2^_offset
                }
            }

            #inline
            fn read_bits(_stat, _offset, _bits) {
              _temp = _stat
              _temp /= 2^_offset
              _temp %= 2^_bits
              return _temp
            }

            #inline
            fn write_bits(_stat, _offset, _value, _prev) {
              _stat -= _prev * 2^_offset
              _stat += _value * 2^_offset
            }
        """.trimIndent())

    }

    @Test
    fun dungeons2() {
        val result = interpret("""
                fn main() {
                    flag = true
                    dgn_move(1, 3, 3)
                }
            
                fn dgn_move(_door_id, _doors_in_room, _sampler_id) {

                    if (flag == false) {
                        exit()
                    } else {
                        flag = false
                    }

                    if (dcx_child_index == 0 && _doors_in_room > 2) { // no memory for children allocated yet
                        dcx_alive += _doors_in_room - 2
                        dcx_child_index = dcx_stack_size
                        dcx_stack_size += _doors_in_room - 1
                    }
                    if (dcx_child_index == 0 && dcx_position == 3) {
                        dcx_child_index = dcx_stack_size
                        dcx_stack_size += _doors_in_room - 1
                    }
                    _prev_index = dcx_index

                    _rel_id = _door_id - dcx_read_door(dcx_position, _sampler_id)

                    if (_rel_id < 0) { // upon return, simply shifting back would lead to underflow, so fix that upon leaving
                        _rel_id += _doors_in_room
                        dcx_shift_door(dcx_position, _doors_in_room * -1)
                    }

                    if (_rel_id >= _doors_in_room) { // upon return, simply shifting back would lead to overflow, so fix that upon leaving
                        _rel_id -= _doors_in_room
                        dcx_shift_door(dcx_position, _doors_in_room)
                    }

                    if (_rel_id == 0 && dcx_position == 0) {
                        // moving backwards at position 0

                        dcx_read_ctx(dcx_parent_index) // read context from parent index

                        _door_id = _prev_index - dcx_child_index + 1

                        _door_id += dcx_read_door(dcx_position, _sampler_id) // apply difference from rel id to entrance id to get door id

                    }

                    if (_rel_id == 0) {
                        // moving backwards
                        dcx_position -= 1

                        _door_id = dcx_read_door(dcx_position, _sampler_id) + 1

                    }

                    if (_rel_id == 1 && dcx_position < 3) {
                        // moving forwards
                        dcx_position += 1

                        _door_id = dcx_read_door(dcx_position, _sampler_id)

                    }

                    if (dcx_child_index == 0 && dcx_stack_size == 31) {

                    }

                    // moving forwards into new context

                    _index = dcx_child_index + _rel_id - 1
                    dcx_read_ctx(_index)

                    dcx_parent_index = _prev_index // update parent context

                    _door_id = dcx_read_door(dcx_position, _sampler_id)
                    message("%stat.player/_sampler_id%")
                }


                fn dgn_progress() {
                    if (dcx_show_prog == false) {
                        exit()
                    }
                    if (dcx_stack_min > dcx_stack_size) {
                        _total = dcx_stack_min
                    } else {
                        _total = dcx_stack_size
                    }
                    _current = dcx_stack_size - dcx_alive
                    _percent = _current * 100 / _total
                    action_bar("&bDungeon Progress: &f%stat.player/_percent%%")
                }
                
                fn dcx_read_ctx(_index) {
                    @nothing = 0
                }

                fn dcx_write_ctx() {
                    _dcx_buffer = 0 // ensure buffer is clear
                }

                fn dcx_read_door(_index, _sampler_id) {
                    #for (i in 0..3) {
                        if (_index == ${"\$"}i) { _door_id_2 = dcx_door_${"\$"}i }
                    }

                    if (_door_id_2 == 0) {
                        // we need to generate this room

                        _door_id_2 = output1
                        dcx_write_door(dcx_position, _door_id_2)
                    }
                    return _door_id_2
                }

                fn dcx_write_door(_index, _door_id_3) {
                    #for (i in 0..3) {
                        if (_index == ${"\$"}i) { dcx_door_${"\$"}i = _door_id_3 }
                    }
                }

                fn dcx_shift_door(_index, _amount) {
                    #for (i in 0..3) {
                        if (_index == ${"\$"}i) { dcx_door_${"\$"}i += _amount }
                    }
                }

        """.trimIndent())

        assertEquals("3", result, "Wrong result")
    }

}