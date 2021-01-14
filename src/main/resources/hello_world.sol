pragma solidity >=0.4.22 <0.6.0;

contract HelloWorld {
    // the contract's owner, set in the constructor
    address owner;

    constructor() public {
        // set the owner of the contract for `kill()`
        owner = msg.sender;
    }

    // return a string
    function greet() public returns (string memory) {
        if (msg.sender == owner) return "Hello, world!";
        else return "Oops!";
    }

    function twoPara(int8 a, int8 b) public returns (string memory) {
        if (a == 1 && b == 1) return "Correct";
        else return "Error";
    }

    // recover the funds of the contract
    function kill() public { if (msg.sender == owner) selfdestruct(msg.sender); }
}