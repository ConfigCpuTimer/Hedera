pragma solidity ^0.4.0;

contract AuctionTest {
    int8[] biddings;

    function AuctionTest(){
        delete biddings;
        biddings.length = 0;
    }

    function submitBid(int8 _price) public {
        biddings.push(_price);
    }

    function marketClear() public returns(int8) {
        return int8 ((biddings[0] + biddings[biddings.length - 1]) / 2);
    }
}
