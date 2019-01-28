package com.example.engyousef.startjoe

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets_ticket.view.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private var database = FirebaseDatabase.getInstance()
    private var myRef = database.reference
    var ListTweets= ArrayList<ticket>()
    var adpater:MyTweetAdpater?=null
    var myemail:String?=null
    var UserUID:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var b:Bundle =intent.extras
        myemail=b.getString("email")
        UserUID=b.getString("uid")
        //Dummy data
        ListTweets.add(ticket("0","him","url","add"))

        adpater = MyTweetAdpater(this,ListTweets)
        lvTweets.adapter=adpater

        LoadPost()
    }

    inner class  MyTweetAdpater: BaseAdapter {
        var listNotesAdpater=ArrayList<ticket>()
        var context: Context?=null
        constructor(context:Context, listNotesAdpater:ArrayList<ticket>):super(){
            this.listNotesAdpater=listNotesAdpater
            this.context=context
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {

            var mytweet=listNotesAdpater[p0]

            if(mytweet.tweetPersonUID.equals("add")){
                var myView=layoutInflater.inflate(R.layout.add_ticket,null)


                myView.iv_attach.setOnClickListener(View.OnClickListener {
                  loadImage()
                })

                myView.iv_post.setOnClickListener(View.OnClickListener {


                    myRef.child("posts").push().setValue(
                        PostInfo(UserUID!!,
                            myView.etPost.text.toString(),DownloadURL!!))
                    myView.etPost.setText("")
                })
                return myView
            }
            else if(mytweet.tweetPersonUID.equals("loading")){
                var myView=layoutInflater.inflate(R.layout.loading_ticket,null)
                return myView
            }else{
                var myView=layoutInflater.inflate(R.layout.tweets_ticket,null)
                 myView.txt_tweet.setText(mytweet.tweetText)
            //  myView.tweet_picture.setImageURI(mytweet.tweetImageURL)
                Picasso.get().load(mytweet.tweetImageURL).into( myView.tweet_picture)

                myRef.child("users").child(mytweet.tweetPersonUID!!)
                    .addValueEventListener(object :ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            try{


                                var td= dataSnapshot!!.value as HashMap<String,Any>

                                for(key in td.keys){
                                    var userInfo = td[key] as String
                                    if(key.equals("ProfileImage")){
                                        Picasso.get().load(userInfo).into( myView.picture_path)

                                    }else{
                                        myView.txtUserName.setText(userInfo)

                                    }

                                }
                            }catch(ex:Exception){

                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }
                    })

                return myView
            }


        }

        override fun getItem(p0: Int): Any {
            return listNotesAdpater[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {

            return listNotesAdpater.size

        }



    }

    val pick_Image_Code=123
    fun loadImage(){
        var intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, pick_Image_Code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==pick_Image_Code && data!=null && resultCode==RESULT_OK){
            val selectedImage = data.data
            val filePathColum= arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage,filePathColum,null,null,null)
            cursor.moveToFirst()
            val columIndex= cursor.getColumnIndex(filePathColum[0])
            val picturePath = cursor.getString(columIndex)
            cursor.close()
            UploadImage(BitmapFactory.decodeFile(picturePath))
        }
    }

    var DownloadURL:String?=""
    fun UploadImage(bitmap:Bitmap){

        ListTweets.add(0,ticket("0","him","url","loading"))
        adpater!!.notifyDataSetChanged()
        val storage= FirebaseStorage.getInstance()
        val storageRef=storage.getReferenceFromUrl("gs://tictactoe-f82e5.appspot.com")
        val df= SimpleDateFormat("ddMMyyHHmmss")
        val dataobj= Date()
        val imagePath=SplitString(myemail!!) + "." + df.format(dataobj)+".jpg"
        val ImageRef= storageRef.child("imagesPost/"+imagePath)


        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data = baos.toByteArray()

        val uploadTask = ImageRef.putBytes(data)
        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext,"fail to upload", Toast.LENGTH_LONG).show()

        }.addOnSuccessListener {taskSnapshot ->
            DownloadURL= taskSnapshot.getMetadata()!!.getReference()!!.getDownloadUrl().toString()

            ListTweets.removeAt(0)
            adpater!!.notifyDataSetChanged()

        }
    }

    fun SplitString(email:String):String{
        val split= email.split("@")
        return split[0]
    }

    fun LoadPost(){
        myRef.child("posts")
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                  try{

                      ListTweets.clear()
                      ListTweets.add(ticket("0","him","url","add"))

                      var td= dataSnapshot!!.value as HashMap<String,Any>

                      for(key in td.keys){
                          var post = td[key] as HashMap<String,Any>
                          ListTweets.add(ticket(key,post["text"] as String,
                              post["postImage"] as String,
                              post["userUID"] as String))

                      }
                      adpater!!.notifyDataSetChanged()
                  }catch(ex:Exception){

                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })


    }
}
