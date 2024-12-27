package com.example.ksptest

import com.example.kspbuilder.GenerateBuilder

@GenerateBuilder
class Person(val id: Int, val name: String, val age: Int, val address: Address?)

@GenerateBuilder
class Address(val id: Int, val name: String, val country: String)