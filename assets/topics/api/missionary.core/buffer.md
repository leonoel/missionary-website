# `missionary.core/buffer`

## Usage
* `(buffer capacity flow)`

`capacity` must be a positive integer.

## Description
A function [operator](/operators.html) 

## Examples

## Synchronicity
* when internal queue is empty, `buffer` step and done are synchronous with `flow` step and done.
* when internal queue is not empty, `buffer` step and done are synchronous with `flow` transfer.
* when internal queue is full, `flow` transfer is synchronous with `buffer` transfer.
* when internal queue is not full, `flow` transfer is synchronous with `flow` step.