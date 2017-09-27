package a.erubit.platform.android;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import a.erubit.platform.R;
import a.erubit.platform.course.Course;
import a.erubit.platform.course.CourseManager;
import a.erubit.platform.course.Lesson;
import a.erubit.platform.course.SetLesson;
import a.erubit.platform.course.WelcomeLesson;

public class ProgressFragment extends Fragment {

    private LessonsFragment.OnLessonInteractionListener mListener;

    public ProgressFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView listView = view.findViewById(android.R.id.list);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setLayoutManager(layoutManager);

        final Course course = CourseManager.i().getCourse(getArguments().getString("id"));
        ((TextView)view.findViewById(R.id.courseName)).setText(course.name);
        listView.setAdapter(new ProgressListAdapter(course));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof LessonsFragment.OnLessonInteractionListener)
            mListener = (LessonsFragment.OnLessonInteractionListener) context;
    }


    private class ProgressListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<ListItem> mList;

        ProgressListAdapter(final Course course) {
            mList = new ArrayList<>(10);

            for (Lesson lesson: course.getLessons()) {
                mList.add(new HeaderItem(lesson));
                if (lesson instanceof WelcomeLesson) {
                    mList.add(new ContentItem(lesson.getProgress().getExplanation(), ""));
                }
                if (lesson instanceof SetLesson) { // also PhraseLesson and CharacterLesson here
                    ((SetLesson)lesson).loadHeavyContent();
                    for (SetLesson.Item item: ((SetLesson)lesson).mSet) {
                        mList.add(new ContentItem(item.character, getKnowledgeText(item.knowledgeLevel)));
                    }
                }
            }
        }

        private String getKnowledgeText(int knowledgeLevel) {
            if (knowledgeLevel > 0 && knowledgeLevel < SetLesson.RANK_FAMILIAR)
                return App.getContext().getString(R.string.studying);
            else if (knowledgeLevel >= SetLesson.RANK_FAMILIAR && knowledgeLevel < SetLesson.RANK_LEARNED)
                return App.getContext().getString(R.string.familiar);
            else if (knowledgeLevel >= SetLesson.RANK_LEARNED && knowledgeLevel < SetLesson.RANK_LEARNED_WELL)
                return App.getContext().getString(R.string.learned);
            else if (knowledgeLevel >= SetLesson.RANK_LEARNED_WELL)
                return App.getContext().getString(R.string.learned_well);
            else
                return App.getContext().getString(R.string.unknown);
        }

        @Override
        public int getItemViewType(int position) {
            return mList.get(position).getType();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            switch (viewType) {
                case ListItem.HEADER:
                    View view = inflater.inflate(R.layout.item_progress_header, parent, false);
                    return new HeaderViewHolder(view);
                case ListItem.CONTENT:
                    view = inflater.inflate(R.layout.item_progress_content, parent, false);
                    return new ContentViewHolder(view);
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).mLesson = ((HeaderItem) mList.get(position)).mLesson;
                String text = ((HeaderItem) mList.get(position)).mName;
                ((HeaderViewHolder) holder).textTitle.setText(text);
            }
            if (holder instanceof ContentViewHolder) {
                ContentItem item = ((ContentItem) mList.get(position));
                ((ContentViewHolder) holder).textTitle.setText(item.mText);
                ((ContentViewHolder) holder).textKnowledge.setText(item.mKnowledge);
            }
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        private class HeaderViewHolder extends RecyclerView.ViewHolder {
            Lesson mLesson;
            private final TextView textTitle;
            private final Button btnPractice;

            HeaderViewHolder(View itemView) {
                super(itemView);
                
                textTitle = itemView.findViewById(android.R.id.text1);
                btnPractice = itemView.findViewById(android.R.id.button1);
                btnPractice.setOnClickListener(view -> mListener.onLessonInteraction(mLesson, LessonsFragment.LessonInteractionAction.PRACTICE));
            }
        }

        private class ContentViewHolder extends RecyclerView.ViewHolder {
            private final TextView textTitle;
            private final TextView textKnowledge;

            ContentViewHolder(View itemView) {
                super(itemView);

                textTitle = itemView.findViewById(android.R.id.text1);
                textKnowledge = itemView.findViewById(android.R.id.text2);
            }
        }

        abstract class ListItem {
            static final int HEADER = 0;
            static final int CONTENT = 1;

            abstract public int getType();
        }

        private class HeaderItem extends ListItem {
            private final Lesson mLesson;
            private final String mName;

            HeaderItem(Lesson lesson) {
                this.mLesson = lesson;
                this.mName = lesson.name;
            }

            @Override
            public int getType() {
                return HEADER;
            }
        }

        private class ContentItem extends ListItem {
            private final String mText;
            private final String mKnowledge;

            ContentItem(String text, String knowledge) {
                mText = text;
                mKnowledge = knowledge;
            }

            @Override
            public int getType() {
                return CONTENT;
            }
        }
    }
}
