pragma solidity ^0.4.0;

contract CommonAuction {
    int8[] _Prices;

    function CommonAuction(){

    }

    function generationBid(int8 _quantity/*, int8 _price*/) public  {
            _Prices.push(_quantity);


        // marketClearing();
    }

    function consumptionBid(int8 _quantity/*, int8 _price*/) public  {
        _Prices.push(_quantity);


        // marketClearing();

    }

    function marketClearingTest() public returns(int8) {
        return (_Prices[0] + _Prices[1]) / 2;
    }
}
