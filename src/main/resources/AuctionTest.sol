pragma solidity ^0.4.24;

contract AuctionTest {
    int[] public biddings;

    function submitBid(int _price) public {
        biddings.push(_price);
    }

    function getLength() view public returns(uint) {
        return biddings.length;
    }

    function marketClear() view public returns(int) {
        return (biddings[0]);
    }

    function marketClearTwo() view public returns(int) {
        return (biddings[biddings.length - 1]);
    }
}
