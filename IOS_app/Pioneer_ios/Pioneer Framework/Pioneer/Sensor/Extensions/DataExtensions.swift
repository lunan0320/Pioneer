//
//  DataExtensions.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import Accelerate

public extension Data {
    
    //将Data型数据转换为十六进制编码格式的字符串
    var hexEncodedString: String { get {
        return map { String(format: "%02hhX", $0) }.joined()
    }}
    
    init?(hexEncodedString: String) {
        guard hexEncodedString.count.isMultiple(of: 2) else {
            return nil
        }
        if hexEncodedString.count == 0 {
            self.init()
        } else {
            let chars = hexEncodedString.map { $0 }
            let bytes = stride(from: 0, to: chars.count, by: 2)
                .map { String(chars[$0]) + String(chars[$0 + 1]) }
                .compactMap { UInt8($0, radix: 16) }
            guard hexEncodedString.count / bytes.count == 2 else {
                return nil
            }
            self.init(bytes)
        }
    }
    
    // MARK:-将内置类型转换为Data类型
    
    //使用mutating关键词修饰后可以修改值引用如结构体自身的属性
    //使用append方法将不同位数与不同类型的数据（每8位作为一个字节）存入到Data实例中
    mutating func append(_ value: UInt8) {
        append(Data([value.bigEndian]))  //此处append方法为向Data实例中添加字节的具体实现方法
    }
    
    mutating func append(_ value: UInt16) {
        //此后对于UInt数据处理的value方法append中的append函数均为第一个append函数的调用（即代码40行）
        append(UInt8(value & 0xFF).bigEndian) // LSB，最低字节
        append(UInt8((value >> 8) & 0xFF).bigEndian) // MSB，最高字节
    }
    
    mutating func append(_ value: UInt32) {
        append(UInt8(value & 0xFF).bigEndian) // LSB
        append(UInt8((value >> 8) & 0xFF).bigEndian)
        append(UInt8((value >> 16) & 0xFF).bigEndian)
        append(UInt8((value >> 24) & 0xFF).bigEndian) // MSB
    }
    
    mutating func append(_ value: UInt64) {
        append(UInt8(value & 0xFF).bigEndian) // LSB
        append(UInt8((value >> 8) & 0xFF).bigEndian)
        append(UInt8((value >> 16) & 0xFF).bigEndian)
        append(UInt8((value >> 24) & 0xFF).bigEndian)
        append(UInt8((value >> 32) & 0xFF).bigEndian)
        append(UInt8((value >> 40) & 0xFF).bigEndian)
        append(UInt8((value >> 48) & 0xFF).bigEndian)
        append(UInt8((value >> 56) & 0xFF).bigEndian) // MSB
    }
    
    //以下方法与上方法作用类似：对于有符号型value，append方法将value按每8位一个字节（从低位到高位）添加到Data实例中
    mutating func append(_ value: Int8) {
        var int8 = value
        append(Data(bytes: &int8, count: MemoryLayout<Int8>.size))
        //MemoryLayout（unsafe）是一个数据结构，用于保存类的内存配置。类对象的size、stride、alignment都是8个字节，这是因为struct类型是值类型数据，class是对象类型数据，使用MemoryLayout对class类型计算其内存结果实际上是对其class类型的引用指针进行操作！
    }

    mutating func append(_ value: Int16) {
        var int16 = value
        append(Data(bytes: &int16, count: MemoryLayout<Int16>.size))
    }

    mutating func append(_ value: Int32) {
        var int32 = value
        append(Data(bytes: &int32, count: MemoryLayout<Int32>.size))
    }

    mutating func append(_ value: Int64) {
        var int64 = value
        append(Data(bytes: &int64, count: MemoryLayout<Int64>.size))
    }
    


    //MARK:-将Data类型还原为内置类型
    
    //从字节组中提取Int8类型（LSB）
    func int8(_ index: Int) -> Int8? {
        guard let value = uint8(index) else {
            return nil
        }
        return Int8(bitPattern: value)
    }
    
    //从字节组中提取UInt8类型（LSB）
    func uint8(_ index: Int) -> UInt8? {
        let bytes = [UInt8](self)
        guard index < bytes.count else {
            return nil
        }
        return bytes[index]
    }
    
    //从字节组中提取Int16类型（LSB）
    func int16(_ index: Int) -> Int16? {
        guard let value = uint16(index) else {
            return nil
        }
        return Int16(bitPattern: value)
    }
    
    //从字节组中提取UInt16类型（LSB）
    func uint16(_ index: Int) -> UInt16? {
        let bytes = [UInt8](self)
        guard index < (bytes.count - 1) else {
            return nil
        }
        return UInt16(bytes[index]) |
            UInt16(bytes[index + 1]) << 8
    }
    
    //从字节组中提取Int32类型（LSB）
    func int32(_ index: Int) -> Int32? {
        guard let value = uint32(index) else {
            return nil
        }
        return Int32(bitPattern: value)
    }
    
    //从字节组中提取UInt32类型（LSB）
    func uint32(_ index: Int) -> UInt32? {
        let bytes = [UInt8](self)
        guard index < (bytes.count - 3) else {
            return nil
        }
        return UInt32(bytes[index]) |
            UInt32(bytes[index + 1]) << 8 |
            UInt32(bytes[index + 2]) << 16 |
            UInt32(bytes[index + 3]) << 24
    }

    //从字节组中提取Int64类型（LSB）
    func int64(_ index: Int) -> Int64? {
        guard let value = uint64(index) else {
            return nil
        }
        return Int64(bitPattern: value)
    }
    
    //从字节组中提取UInt64类型（LSB）
    func uint64(_ index: Int) -> UInt64? {
        let bytes = [UInt8](self)
        guard index < (bytes.count - 7) else {
            return nil
        }
        return UInt64(bytes[index]) |
            UInt64(bytes[index + 1]) << 8 |
            UInt64(bytes[index + 2]) << 16 |
            UInt64(bytes[index + 3]) << 24 |
            UInt64(bytes[index + 4]) << 32 |
            UInt64(bytes[index + 5]) << 40 |
            UInt64(bytes[index + 6]) << 48 |
            UInt64(bytes[index + 7]) << 56
    }
    
}

