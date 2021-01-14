pragma solidity >=0.4.22 <0.6.0;

contract HelloWorld {
    // the contract's owner, set in the constructor
    address owner;
    int8[] prices;

    constructor() public {
        // set the owner of the contract for `kill()`
        owner = msg.sender;
    }

    // return a string
    function greet() public returns (string memory) {
        if (msg.sender == owner) return "Hello, world!";
        else return "Oops!";
    }

    function bidding(int8 _value) public returns(int8) {
//        prices.push(_value);
        return _value;
    }

    function getResult() public returns(int8) {
        return 10;
    }

    // recover the funds of the contract
    function kill() public { if (msg.sender == owner) selfdestruct(msg.sender); }
}