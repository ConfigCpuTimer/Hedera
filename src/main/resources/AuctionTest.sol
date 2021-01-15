pragma solidity ^0.4.0;

contract AuctionTest {
    int[] biddings;

    function AuctionTest(){
    }

    function submitBid(int _price) public {
        biddings.push(_price);
    }

    function marketClear() public returns(int) {
        return int8 (biddings[0] +biddings[1]);
    }
}
