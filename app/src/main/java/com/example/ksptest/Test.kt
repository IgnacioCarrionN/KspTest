package com.example.ksptest

class Test {
    fun createPerson() {
        val person = personBuilder {
            setId(10)
            setName("Test")
            setAge(100)
            setAddress(
                addressBuilder {
                    setId(10)
                    setName("AddressTest")
                    setCountry("Spain")
                }
            )
        }
    }
}