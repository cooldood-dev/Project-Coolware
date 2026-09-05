package com.github.cooldood.events.impl;

import com.github.cooldood.events.Event;
import lombok.AllArgsConstructor;
import net.minecraft.util.MovementInput;

/*
Now beat that!
And your mother's saying "Go to college"
So you finish college and it's wonderful
You feel so good
And after all the partying and craziness
And don't forget about that drug habit you picked up at school being around your peers
Hey!
Now you'll get that 25-thousand dollar job a year
And you'll spend all your money on crack cocaine
But, it'll be YOUR money
No more borrowing from mom for my high!
So now you get your degree tattooed on your back
You're so excited about it
If you continue to work at the Gap, after several interviews, oh my God
You'll come in at a entry-level position
And when you do that
If you kiss enough ass
You'll move up to the next level
Which is being the secretary's secretary
And boy, is that great
You get to take messages for the secretary
Who NEVER went to college
She's actually the boss's niece!
So now you're part of the family
You know what college does for you?
It makes you really smart, man
All you kids wanted to talk in the back of the class
Not me, I listened
Okay?
I was a hall monitor
This was meant to be
You know how many classes I took?
Extra classes, extra classes
No, I've never had sex
But you know what?
My degree keeps me satisfied
When a lady walks up to me and says
"Hey, you know what's sexy?"
I said, no, I don't know what it is
But I bet I can add up all the change in your purse
Very fast
 */
@AllArgsConstructor
public class MovementInputEvent extends Event {
    public MovementInput movementInput;
}
